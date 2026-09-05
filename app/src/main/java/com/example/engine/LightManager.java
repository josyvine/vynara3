package com.example.engine;

import java.util.ArrayList;
import java.util.List;

public class LightManager {
    private final List<Light> lights = new ArrayList<>();

    public LightManager() {
        setupDefaultDaylight();
    }

    public void setupDefaultDaylight() {
        lights.clear();

        // Directional Sun Light
        Light sun = new Light("light_sun", Light.Type.DIRECTIONAL);
        sun.setPosition(8f, 15f, 10f);
        sun.setDirection(-0.5f, -1.0f, -0.5f);
        sun.setColor(1.0f, 0.95f, 0.85f); // Warm sun
        sun.setIntensity(1.2f);
        lights.add(sun);

        // Fill Ambient Light
        Light ambient = new Light("light_ambient", Light.Type.AMBIENT);
        ambient.setColor(0.7f, 0.8f, 1.0f); // Cool sky ambient
        ambient.setIntensity(0.4f);
        lights.add(ambient);
    }

    /**
     * Phase 17 Alignment: Configures warm evening lighting rig with golden sun 
     * and low-intensity dusk ambient.
     */
    public void setupEveningLighting() {
        lights.clear();

        Light eveningSun = new Light("light_sun_evening", Light.Type.DIRECTIONAL);
        eveningSun.setPosition(15f, 3f, 8f);
        eveningSun.setDirection(-0.9f, -0.2f, -0.4f);
        eveningSun.setColor(1.0f, 0.45f, 0.2f); // Golden sunset orange
        eveningSun.setIntensity(1.4f);
        lights.add(eveningSun);

        Light eveningAmbient = new Light("light_ambient_dusk", Light.Type.AMBIENT);
        eveningAmbient.setColor(0.2f, 0.25f, 0.4f); // Deep dusk blue
        eveningAmbient.setIntensity(0.25f);
        lights.add(eveningAmbient);
    }

    /**
     * Configures classic 3D studio 3-Point Lighting (Key Light, Fill Light, Back Light).
     */
    public void setupStudioThreePointLighting() {
        lights.clear();

        // Key Light
        Light keyLight = new Light("light_key", Light.Type.POINT);
        keyLight.setPosition(5f, 6f, 5f);
        keyLight.setColor(1.0f, 0.98f, 0.9f);
        keyLight.setIntensity(1.3f);
        lights.add(keyLight);

        // Fill Light
        Light fillLight = new Light("light_fill", Light.Type.POINT);
        fillLight.setPosition(-5f, 3f, 4f);
        fillLight.setColor(0.6f, 0.7f, 0.9f);
        fillLight.setIntensity(0.6f);
        lights.add(fillLight);

        // Rim / Back Light
        Light backLight = new Light("light_back", Light.Type.POINT);
        backLight.setPosition(0f, 8f, -6f);
        backLight.setColor(1.0f, 1.0f, 1.0f);
        backLight.setIntensity(0.9f);
        lights.add(backLight);

        Light ambient = new Light("light_ambient_studio", Light.Type.AMBIENT);
        ambient.setColor(0.3f, 0.3f, 0.35f);
        ambient.setIntensity(0.3f);
        lights.add(ambient);
    }

    public void addLight(Light light) {
        if (light != null && !lights.contains(light)) {
            lights.add(light);
        }
    }

    public boolean removeLight(String lightId) {
        if (lightId == null) return false;
        return lights.removeIf(l -> l.getId().equalsIgnoreCase(lightId));
    }

    public Light findLightById(String lightId) {
        if (lightId == null) return null;
        for (Light l : lights) {
            if (l.getId().equalsIgnoreCase(lightId)) return l;
        }
        return null;
    }

    public Light getPrimaryDirectionalLight() {
        for (Light l : lights) {
            if (l.getType() == Light.Type.DIRECTIONAL) return l;
        }
        return lights.isEmpty() ? null : lights.get(0);
    }

    public Light getAmbientLight() {
        for (Light l : lights) {
            if (l.getType() == Light.Type.AMBIENT) return l;
        }
        return null;
    }

    public List<Light> getLights() { return lights; }

    public void clearLights() {
        lights.clear();
    }
}