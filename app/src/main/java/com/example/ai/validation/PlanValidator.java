package com.example.ai.validation;

import com.example.ai.protocol.AIToolCall;
import com.example.tools.ToolRegistry;
import com.example.utils.VynaraLogger;
import com.example.utils.VynaraLogger.LogLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlanValidator {

    private final ToolRegistry toolRegistry;

    public PlanValidator(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry != null ? toolRegistry : new ToolRegistry();
    }

    /**
     * CORE CONTRACT ENFORCER: Inspects raw Gemini tool calls, resolves registered commands,
     * and dynamically translates unregistered high-level capabilities into valid execution nodes.
     */
    public List<AIToolCall> validateAndMap(List<AIToolCall> rawCalls) {
        List<AIToolCall> validatedCalls = new ArrayList<>();
        if (rawCalls == null) return validatedCalls;

        VynaraLogger.ai("Initializing 3D production plan validation contract...");

        for (AIToolCall call : rawCalls) {
            if (call == null) continue;

            String rawToolId = call.getToolId();
            VynaraLogger.ai("Parsing tool call: " + rawToolId);

            // 1. Check if the proposed command is actively registered in the ToolRegistry
            if (toolRegistry.getTool(rawToolId) != null) {
                VynaraLogger.toolManifest("Command verified in authoritative registry: " + rawToolId);
                validatedCalls.add(call);
                continue;
            }

            // 2. Unregistered command encountered. Evaluate if it matches a known high-level Capability
            VynaraLogger.validator(LogLevel.WARNING, "INVALID TOOL ID DETECTED: '" + rawToolId + "' is not a registered executable command.");

            AIToolCall remappedCall = tryResolveCapabilityToTool(call);
            if (remappedCall != null) {
                // 3. Double-check that the newly resolved target tool is actually registered in the local manifest
                if (toolRegistry.getTool(remappedCall.getToolId()) != null) {
                    VynaraLogger.mapper("Dynamic Contract Mapping: Remapped capability '" + rawToolId + "' to registered tool '" + remappedCall.getToolId() + "'");
                    validatedCalls.add(remappedCall);
                } else {
                    VynaraLogger.validator(LogLevel.ERROR, "CRITICAL ERROR: Remapped tool ID '" + remappedCall.getToolId() + "' is missing from active registry!");
                    throw new IllegalArgumentException("Rigging violation: Mapped target command '" + remappedCall.getToolId() + "' is unregistered.");
                }
            } else {
                // 4. Contract Violation: Proposed token is neither a registered tool nor a mappable capability
                VynaraLogger.validator(LogLevel.ERROR, "CONTRACT FAILURE: Unmapped token '" + rawToolId + "' rejected. Execution graph halted.");
                throw new IllegalArgumentException("Contract violation: Unrecognized capability or tool ID '" + rawToolId + "'");
            }
        }

        VynaraLogger.ai("Production plan validation completed. 3D DAG prepared for compilation.");
        return validatedCalls;
    }

    /**
     * Maps high-level metadata capabilities to actual executable tool structures
     * and injects the corresponding required parameters.
     */
    private AIToolCall tryResolveCapabilityToTool(AIToolCall original) {
        if (original.getToolId() == null) return null;

        String rawId = original.getToolId().toLowerCase().trim();
        AIToolCall mapped = null;

        if ("procedural_vegetation_generation".equals(rawId)) {
            VynaraLogger.knowledge("Capability lookup: '" + rawId + "' maps to procedural foliage builder.");
            mapped = new AIToolCall("geometry.create_procedural", "Dynamic remapped vegetation generator");
            mapped.getParameters().put("type", "tree");
            mapped.getParameters().put("name", "Oak Tree");

        } else if ("water_transmission_shader".equals(rawId)) {
            VynaraLogger.knowledge("Capability lookup: '" + rawId + "' maps to transparent water pool builder.");
            mapped = new AIToolCall("geometry.create_procedural", "Dynamic remapped pool generator");
            mapped.getParameters().put("type", "pool");
            mapped.getParameters().put("name", "Pool");

        } else if ("procedural_architecture".equals(rawId)) {
            VynaraLogger.knowledge("Capability lookup: '" + rawId + "' maps to architectural floor layout builder.");
            mapped = new AIToolCall("geometry.create_procedural", "Dynamic remapped architecture generator");
            mapped.getParameters().put("type", "house");
            mapped.getParameters().put("name", "Villa");

        } else if ("humanoid_generation".equals(rawId)) {
            VynaraLogger.knowledge("Capability lookup: '" + rawId + "' maps to anatomical bipedal mesh builder.");
            mapped = new AIToolCall("character.create_humanoid", "Dynamic remapped humanoid generator");
            mapped.getParameters().put("name", "Hero");
            mapped.getParameters().put("height", 1.8f);
            mapped.getParameters().put("style", "REALISTIC");

        } else if ("rigging".equals(rawId)) {
            VynaraLogger.knowledge("Capability lookup: '" + rawId + "' maps to multi-influence skin weighting.");
            mapped = new AIToolCall("skeleton.bind", "Dynamic remapped rigging binder");

        } else if ("animation".equals(rawId)) {
            VynaraLogger.knowledge("Capability lookup: '" + rawId + "' maps to skeletal keyframe player.");
            mapped = new AIToolCall("animation.create_clip", "Dynamic remapped animation clip applier");
            mapped.getParameters().put("clipName", "walk");
        }

        // Deep-merge original parameters from Gemini over the procedural defaults
        if (mapped != null && original.getParameters() != null) {
            for (Map.Entry<String, Object> entry : original.getParameters().entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    mapped.getParameters().put(entry.getKey(), entry.getValue());
                }
            }
        }

        return mapped;
    }
}