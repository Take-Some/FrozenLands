package org.takesome.frozenlands.engine.player.input;

import com.jme3.renderer.Camera;
import org.takesome.frozenlands.engine.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class UserInputHelper extends UserInputHandler {

    public UserInputHelper(Player player, Camera cam, Runnable attackCallback) {
        super(player, attackCallback);
    }

    /**
     * Get a stack of input map names from the provided userInputConfig.
     *
     * @param userInputConfig The user input configuration map.
     * @return A stack containing the input map names.
     */
    protected static Stack<String> getInputMaps(HashMap<String, List<Object>> userInputConfig) {
        Stack<String> inputMaps = new Stack<>();
        for (String inputArrName : userInputConfig.keySet()) {
            inputMaps.push(inputArrName);
        }
        return inputMaps;
    }

    /**
     * Add a new input map to the userInputConfig.
     *
     * @param inputMapName The name of the new input map to be added.
     * @param inputActions The list of input actions for the new input map.
     */
    public void addInputMap(String inputMapName, List<Object> inputActions) {
        getUserInputConfig().put(inputMapName, inputActions);
    }

    /**
     * Remove an existing input map from the userInputConfig.
     *
     * @param inputMapName The name of the input map to be removed.
     * @return true if the input map was removed, false otherwise.
     */
    public boolean removeInputMap(String inputMapName) {
        return getUserInputConfig().remove(inputMapName) != null;
    }

}