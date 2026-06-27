package org.takesome.frozenlands.engine.player;

import com.jme3.anim.SkinningControl;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class PlayerSkinningGuard {
    private static final VertexBuffer.Type[] INDEX_BUFFERS = {
            VertexBuffer.Type.BoneIndex,
            VertexBuffer.Type.HWBoneIndex
    };

    private PlayerSkinningGuard() {
    }

    static RepairResult repairMultiSkinTargets(Spatial root) {
        List<ControlBinding> controls = new ArrayList<>();
        List<Geometry> geometries = new ArrayList<>();
        collect(root, controls, geometries);

        if (controls.size() <= 1) {
            return RepairResult.none("single-or-no-skinning-control");
        }
        if (geometries.isEmpty()) {
            return RepairResult.none("no-geometries");
        }

        Map<Geometry, ControlBinding> assignments = new LinkedHashMap<>();
        for (Geometry geometry : geometries) {
            int maxBoneIndex = maxBoneIndex(geometry.getMesh());
            if (maxBoneIndex < 0) {
                continue;
            }

            ControlBinding control = chooseControlForGeometry(controls, geometry, maxBoneIndex);
            if (control == null) {
                return RepairResult.failed(
                        controls.size(),
                        geometries.size(),
                        assignments.size(),
                        "no-compatible-skinning-control geometry=" + nameOf(geometry) + " maxBoneIndex=" + maxBoneIndex
                );
            }
            assignments.put(geometry, control);
        }

        if (assignments.isEmpty()) {
            return RepairResult.none("no-skinned-geometries");
        }

        for (ControlBinding binding : controls) {
            Spatial owner = binding.owner();
            if (owner != null) {
                owner.removeControl(binding.control());
            }
        }

        int installed = 0;
        for (Map.Entry<Geometry, ControlBinding> entry : assignments.entrySet()) {
            Geometry geometry = entry.getKey();
            ControlBinding binding = entry.getValue();

            geometry.removeControl(SkinningControl.class);
            SkinningControl perGeometryControl = new SkinningControl(binding.control().getArmature());
            perGeometryControl.setHardwareSkinningPreferred(false);
            geometry.addControl(perGeometryControl);
            installed++;
        }

        return RepairResult.repaired(controls.size(), geometries.size(), installed);
    }

    static Result inspect(Spatial root) {
        List<ControlBinding> controls = new ArrayList<>();
        List<Geometry> geometries = new ArrayList<>();
        collect(root, controls, geometries);

        if (controls.isEmpty()) {
            return Result.notSkinned(geometries.size());
        }

        IdentityHashMap<Mesh, Geometry> geometryByMesh = new IdentityHashMap<>();
        for (Geometry geometry : geometries) {
            if (geometry.getMesh() != null) {
                geometryByMesh.put(geometry.getMesh(), geometry);
            }
        }

        int checkedGeometryCount = 0;
        int checkedBufferCount = 0;
        int maxJointCount = 0;
        int maxBoneIndex = -1;

        for (ControlBinding binding : controls) {
            int jointCount = binding.jointCount();
            maxJointCount = Math.max(maxJointCount, jointCount);
            Mesh[] targets = binding.control().getTargets();
            if (targets == null) {
                continue;
            }

            for (Mesh mesh : targets) {
                if (mesh == null) {
                    continue;
                }
                Geometry geometry = geometryByMesh.get(mesh);
                String geometryName = geometry == null ? "mesh@" + System.identityHashCode(mesh) : nameOf(geometry);

                boolean checkedGeometry = false;
                for (VertexBuffer.Type type : INDEX_BUFFERS) {
                    VertexBuffer buffer = mesh.getBuffer(type);
                    if (buffer == null || buffer.getData() == null) {
                        continue;
                    }
                    checkedGeometry = true;
                    checkedBufferCount++;

                    Scan scan = scanIndexBuffer(buffer, jointCount);
                    maxBoneIndex = Math.max(maxBoneIndex, scan.maxBoneIndex());
                    if (!scan.valid()) {
                        return Result.invalid(
                                controls.size(),
                                geometries.size(),
                                checkedGeometryCount,
                                checkedBufferCount,
                                maxJointCount,
                                maxBoneIndex,
                                geometryName,
                                type.name(),
                                scan.invalidBoneIndex(),
                                scan.invalidElement(),
                                scan.invalidComponent(),
                                scan.reason()
                        );
                    }
                }

                if (checkedGeometry) {
                    checkedGeometryCount++;
                }
            }
        }

        return Result.valid(controls.size(), geometries.size(), checkedGeometryCount, checkedBufferCount, maxJointCount, maxBoneIndex);
    }

    private static ControlBinding chooseControlForGeometry(List<ControlBinding> controls, Geometry geometry, int maxBoneIndex) {
        String name = nameOf(geometry).toLowerCase();
        boolean clothLike = name.contains("cloth");
        ControlBinding chosen = null;

        for (ControlBinding control : controls) {
            if (control.jointCount() <= maxBoneIndex) {
                continue;
            }
            if (chosen == null) {
                chosen = control;
                continue;
            }
            if (clothLike) {
                if (control.jointCount() < chosen.jointCount()) {
                    chosen = control;
                }
            } else if (control.jointCount() > chosen.jointCount()) {
                chosen = control;
            }
        }

        return chosen;
    }

    private static void collect(Spatial spatial, List<ControlBinding> controls, List<Geometry> geometries) {
        for (int i = 0; i < spatial.getNumControls(); i++) {
            if (spatial.getControl(i) instanceof SkinningControl control) {
                int jointCount = control.getArmature() == null ? 0 : control.getArmature().getJointCount();
                controls.add(new ControlBinding(control, spatial, jointCount, controls.size()));
            }
        }
        if (spatial instanceof Geometry geometry) {
            geometries.add(geometry);
            return;
        }
        if (spatial instanceof Node node) {
            for (Spatial child : node.getChildren()) {
                collect(child, controls, geometries);
            }
        }
    }

    private static int maxBoneIndex(Mesh mesh) {
        if (mesh == null) {
            return -1;
        }
        int max = -1;
        for (VertexBuffer.Type type : INDEX_BUFFERS) {
            VertexBuffer buffer = mesh.getBuffer(type);
            if (buffer == null || buffer.getData() == null || buffer.getNumComponents() <= 0) {
                continue;
            }
            int components = buffer.getNumComponents();
            int elements = buffer.getNumElements();
            for (int element = 0; element < elements; element++) {
                for (int component = 0; component < components; component++) {
                    max = Math.max(max, readIndex(buffer, element, component));
                }
            }
        }
        return max;
    }

    private static Scan scanIndexBuffer(VertexBuffer buffer, int jointCount) {
        if (buffer.getData() == null) {
            return Scan.invalid(-1, -1, -1, -1, "missing-buffer-data");
        }
        if (buffer.getNumComponents() <= 0) {
            return Scan.invalid(-1, -1, -1, -1, "invalid-component-count");
        }
        if (jointCount <= 0) {
            return Scan.invalid(-1, -1, -1, -1, "missing-armature-joints");
        }

        int maxBoneIndex = -1;
        int components = buffer.getNumComponents();
        int elements = buffer.getNumElements();
        for (int element = 0; element < elements; element++) {
            for (int component = 0; component < components; component++) {
                int boneIndex = readIndex(buffer, element, component);
                maxBoneIndex = Math.max(maxBoneIndex, boneIndex);
                if (boneIndex < 0) {
                    return Scan.invalid(maxBoneIndex, boneIndex, element, component, "negative-bone-index");
                }
                if (boneIndex >= jointCount) {
                    return Scan.invalid(maxBoneIndex, boneIndex, element, component, "bone-index-out-of-armature-range");
                }
            }
        }
        return Scan.valid(maxBoneIndex);
    }

    private static int readIndex(VertexBuffer buffer, int element, int component) {
        Object value = buffer.getElementComponent(element, component);
        if (!(value instanceof Number number)) {
            return -1;
        }

        int raw = number.intValue();
        return switch (buffer.getFormat()) {
            case UnsignedByte -> raw & 0xFF;
            case UnsignedShort, Half -> raw & 0xFFFF;
            case UnsignedInt -> Integer.toUnsignedLong(raw) > Integer.MAX_VALUE ? Integer.MAX_VALUE : raw;
            case Float, Double -> Math.round(number.floatValue());
            default -> raw;
        };
    }

    private static String nameOf(Spatial spatial) {
        return spatial == null || spatial.getName() == null ? "unnamed" : spatial.getName();
    }

    private record ControlBinding(SkinningControl control, Spatial owner, int jointCount, int ordinal) {
    }

    private record Scan(boolean valid, int maxBoneIndex, int invalidBoneIndex, int invalidElement, int invalidComponent, String reason) {
        static Scan valid(int maxBoneIndex) {
            return new Scan(true, maxBoneIndex, -1, -1, -1, "ok");
        }

        static Scan invalid(int maxBoneIndex, int invalidBoneIndex, int invalidElement, int invalidComponent, String reason) {
            return new Scan(false, maxBoneIndex, invalidBoneIndex, invalidElement, invalidComponent, reason);
        }
    }

    record RepairResult(boolean repaired, boolean failed, int skinningControlCount, int geometryCount, int assignedGeometryCount, String reason) {
        static RepairResult none(String reason) {
            return new RepairResult(false, false, 0, 0, 0, reason);
        }

        static RepairResult repaired(int skinningControlCount, int geometryCount, int assignedGeometryCount) {
            return new RepairResult(true, false, skinningControlCount, geometryCount, assignedGeometryCount, "multi-skin-targets-regrouped");
        }

        static RepairResult failed(int skinningControlCount, int geometryCount, int assignedGeometryCount, String reason) {
            return new RepairResult(false, true, skinningControlCount, geometryCount, assignedGeometryCount, reason);
        }

        String summary() {
            return "repaired=" + repaired
                    + " failed=" + failed
                    + " skinningControls=" + skinningControlCount
                    + " geometryCount=" + geometryCount
                    + " assignedGeometryCount=" + assignedGeometryCount
                    + " reason=" + reason;
        }
    }

    record Result(
            boolean skinned,
            boolean valid,
            int skinningControlCount,
            int geometryCount,
            int checkedGeometryCount,
            int checkedBufferCount,
            int maxJointCount,
            int maxBoneIndex,
            String invalidGeometry,
            String invalidBufferType,
            int invalidBoneIndex,
            int invalidElement,
            int invalidComponent,
            String reason
    ) {
        static Result notSkinned(int geometryCount) {
            return new Result(false, true, 0, geometryCount, 0, 0, 0, -1, "", "", -1, -1, -1, "not-skinned");
        }

        static Result valid(int skinningControlCount, int geometryCount, int checkedGeometryCount, int checkedBufferCount, int maxJointCount, int maxBoneIndex) {
            return new Result(true, true, skinningControlCount, geometryCount, checkedGeometryCount, checkedBufferCount, maxJointCount, maxBoneIndex, "", "", -1, -1, -1, "ok");
        }

        static Result invalid(int skinningControlCount, int geometryCount, int checkedGeometryCount, int checkedBufferCount, int maxJointCount, int maxBoneIndex, String invalidGeometry, String invalidBufferType, int invalidBoneIndex, int invalidElement, int invalidComponent, String reason) {
            return new Result(true, false, skinningControlCount, geometryCount, checkedGeometryCount, checkedBufferCount, maxJointCount, maxBoneIndex, invalidGeometry, invalidBufferType, invalidBoneIndex, invalidElement, invalidComponent, reason);
        }

        boolean disablesVisual() {
            return skinned && !valid;
        }

        String summary() {
            if (!skinned) {
                return "not-skinned geometryCount=" + geometryCount;
            }
            if (valid) {
                return "valid skinningControls=" + skinningControlCount
                        + " geometryCount=" + geometryCount
                        + " checkedGeometryCount=" + checkedGeometryCount
                        + " checkedBufferCount=" + checkedBufferCount
                        + " maxJointCount=" + maxJointCount
                        + " maxBoneIndex=" + maxBoneIndex;
            }
            return "invalid reason=" + reason
                    + " geometry=" + invalidGeometry
                    + " buffer=" + invalidBufferType
                    + " invalidBoneIndex=" + invalidBoneIndex
                    + " jointCount=" + maxJointCount
                    + " maxBoneIndex=" + maxBoneIndex
                    + " element=" + invalidElement
                    + " component=" + invalidComponent;
        }
    }
}
