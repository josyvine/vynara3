package com.example.character;

public class SkeletonBuilder {

    /**
     * Phase 9 Alignment: Builds a full anatomical humanoid skeleton hierarchy
     * supporting clavicles, upper arms, forearms, hands, fingers, thighs, calves, feet, and toes.
     */
    public static Skeleton buildHumanoidSkeleton(float height) {
        float h = Math.max(0.5f, height);

        Bone root = new Bone("bone_root", "ROOT");
        Bone pelvis = new Bone("bone_pelvis", "PELVIS");
        pelvis.getLocalTransform().setPosition(0f, h * 0.52f, 0f);
        root.addChild(pelvis);

        // Spine Chain
        Bone spine = new Bone("bone_spine", "SPINE");
        spine.getLocalTransform().setPosition(0f, h * 0.12f, 0f);
        pelvis.addChild(spine);

        Bone chest = new Bone("bone_chest", "CHEST");
        chest.getLocalTransform().setPosition(0f, h * 0.15f, 0f);
        spine.addChild(chest);

        Bone neck = new Bone("bone_neck", "NECK");
        neck.getLocalTransform().setPosition(0f, h * 0.1f, 0f);
        chest.addChild(neck);

        Bone head = new Bone("bone_head", "HEAD");
        head.getLocalTransform().setPosition(0f, h * 0.08f, 0f);
        neck.addChild(head);

        // Left Arm Chain (Clavicle -> Upper Arm -> Forearm -> Hand -> Fingers)
        Bone lClavicle = new Bone("bone_l_clavicle", "LEFT_CLAVICLE");
        lClavicle.getLocalTransform().setPosition(-0.12f, h * 0.1f, 0f);
        chest.addChild(lClavicle);

        Bone lUpperArm = new Bone("bone_l_upper_arm", "LEFT_UPPER_ARM");
        lUpperArm.getLocalTransform().setPosition(-0.15f, 0f, 0f);
        lClavicle.addChild(lUpperArm);

        Bone lForearm = new Bone("bone_l_forearm", "LEFT_FOREARM");
        lForearm.getLocalTransform().setPosition(-0.2f, 0f, 0f);
        lUpperArm.addChild(lForearm);

        Bone lHand = new Bone("bone_l_hand", "LEFT_HAND");
        lHand.getLocalTransform().setPosition(-0.18f, 0f, 0f);
        lForearm.addChild(lHand);

        Bone lFingers = new Bone("bone_l_fingers", "LEFT_FINGERS");
        lFingers.getLocalTransform().setPosition(-0.08f, 0f, 0f);
        lHand.addChild(lFingers);

        // Right Arm Chain
        Bone rClavicle = new Bone("bone_r_clavicle", "RIGHT_CLAVICLE");
        rClavicle.getLocalTransform().setPosition(0.12f, h * 0.1f, 0f);
        chest.addChild(rClavicle);

        Bone rUpperArm = new Bone("bone_r_upper_arm", "RIGHT_UPPER_ARM");
        rUpperArm.getLocalTransform().setPosition(0.15f, 0f, 0f);
        rClavicle.addChild(rUpperArm);

        Bone rForearm = new Bone("bone_r_forearm", "RIGHT_FOREARM");
        rForearm.getLocalTransform().setPosition(0.2f, 0f, 0f);
        rUpperArm.addChild(rForearm);

        Bone rHand = new Bone("bone_r_hand", "RIGHT_HAND");
        rHand.getLocalTransform().setPosition(0.18f, 0f, 0f);
        rForearm.addChild(rHand);

        Bone rFingers = new Bone("bone_r_fingers", "RIGHT_FINGERS");
        rFingers.getLocalTransform().setPosition(0.08f, 0f, 0f);
        rHand.addChild(rFingers);

        // Left Leg Chain (Thigh -> Calf -> Foot -> Toes)
        Bone lThigh = new Bone("bone_l_thigh", "LEFT_THIGH");
        lThigh.getLocalTransform().setPosition(-0.12f, -0.05f, 0f);
        pelvis.addChild(lThigh);

        Bone lCalf = new Bone("bone_l_calf", "LEFT_CALF");
        lCalf.getLocalTransform().setPosition(0f, -h * 0.22f, 0f);
        lThigh.addChild(lCalf);

        Bone lFoot = new Bone("bone_l_foot", "LEFT_FOOT");
        lFoot.getLocalTransform().setPosition(0f, -h * 0.22f, 0.08f);
        lCalf.addChild(lFoot);

        Bone lToes = new Bone("bone_l_toes", "LEFT_TOES");
        lToes.getLocalTransform().setPosition(0f, 0f, 0.12f);
        lFoot.addChild(lToes);

        // Right Leg Chain
        Bone rThigh = new Bone("bone_r_thigh", "RIGHT_THIGH");
        rThigh.getLocalTransform().setPosition(0.12f, -0.05f, 0f);
        pelvis.addChild(rThigh);

        Bone rCalf = new Bone("bone_r_calf", "RIGHT_CALF");
        rCalf.getLocalTransform().setPosition(0f, -h * 0.22f, 0f);
        rThigh.addChild(rCalf);

        Bone rFoot = new Bone("bone_r_foot", "RIGHT_FOOT");
        rFoot.getLocalTransform().setPosition(0f, -h * 0.22f, 0.08f);
        rCalf.addChild(rFoot);

        Bone rToes = new Bone("bone_r_toes", "RIGHT_TOES");
        rToes.getLocalTransform().setPosition(0f, 0f, 0.12f);
        rFoot.addChild(rToes);

        return new Skeleton(root);
    }

    /**
     * Phase 9 Alignment: Builds quadruped animal skeleton topology
     * (dogs, cats, horses).
     */
    public static Skeleton buildQuadrupedSkeleton() {
        Bone root = new Bone("bone_root", "ROOT");
        Bone pelvis = new Bone("bone_pelvis", "PELVIS");
        pelvis.getLocalTransform().setPosition(0f, 0.6f, -0.3f);
        root.addChild(pelvis);

        Bone spine = new Bone("bone_spine", "SPINE");
        spine.getLocalTransform().setPosition(0f, 0f, 0.3f);
        pelvis.addChild(spine);

        Bone chest = new Bone("bone_chest", "CHEST");
        chest.getLocalTransform().setPosition(0f, 0.1f, 0.3f);
        spine.addChild(chest);

        Bone neck = new Bone("bone_neck", "NECK");
        neck.getLocalTransform().setPosition(0f, 0.25f, 0.2f);
        chest.addChild(neck);

        Bone head = new Bone("bone_head", "HEAD");
        head.getLocalTransform().setPosition(0f, 0.2f, 0.15f);
        neck.addChild(head);

        // Front Left Leg
        Bone flUpper = new Bone("bone_fl_upper", "FRONT_LEFT_UPPER_LEG");
        flUpper.getLocalTransform().setPosition(-0.2f, -0.1f, 0.05f);
        chest.addChild(flUpper);

        Bone flLower = new Bone("bone_fl_lower", "FRONT_LEFT_LOWER_LEG");
        flLower.getLocalTransform().setPosition(0f, -0.25f, 0f);
        flUpper.addChild(flLower);

        Bone flFoot = new Bone("bone_fl_foot", "FRONT_LEFT_FOOT");
        flFoot.getLocalTransform().setPosition(0f, -0.25f, 0.05f);
        flLower.addChild(flFoot);

        // Front Right Leg
        Bone frUpper = new Bone("bone_fr_upper", "FRONT_RIGHT_UPPER_LEG");
        frUpper.getLocalTransform().setPosition(0.2f, -0.1f, 0.05f);
        chest.addChild(frUpper);

        Bone frLower = new Bone("bone_fr_lower", "FRONT_RIGHT_LOWER_LEG");
        frLower.getLocalTransform().setPosition(0f, -0.25f, 0f);
        frUpper.addChild(frLower);

        Bone frFoot = new Bone("bone_fr_foot", "FRONT_RIGHT_FOOT");
        frFoot.getLocalTransform().setPosition(0f, -0.25f, 0.05f);
        frLower.addChild(frFoot);

        // Rear Left Leg
        Bone rlUpper = new Bone("bone_rl_upper", "REAR_LEFT_UPPER_LEG");
        rlUpper.getLocalTransform().setPosition(-0.18f, -0.1f, -0.05f);
        pelvis.addChild(rlUpper);

        Bone rlLower = new Bone("bone_rl_lower", "REAR_LEFT_LOWER_LEG");
        rlLower.getLocalTransform().setPosition(0f, -0.25f, 0f);
        rlUpper.addChild(rlLower);

        Bone rlFoot = new Bone("bone_rl_foot", "REAR_LEFT_FOOT");
        rlFoot.getLocalTransform().setPosition(0f, -0.25f, 0.05f);
        rlLower.addChild(rlFoot);

        // Rear Right Leg
        Bone rrUpper = new Bone("bone_rr_upper", "REAR_RIGHT_UPPER_LEG");
        rrUpper.getLocalTransform().setPosition(0.18f, -0.1f, -0.05f);
        pelvis.addChild(rrUpper);

        Bone rrLower = new Bone("bone_rr_lower", "REAR_RIGHT_LOWER_LEG");
        rrLower.getLocalTransform().setPosition(0f, -0.25f, 0f);
        rrUpper.addChild(rrLower);

        Bone rrFoot = new Bone("bone_rr_foot", "REAR_RIGHT_FOOT");
        rrFoot.getLocalTransform().setPosition(0f, -0.25f, 0.05f);
        rrLower.addChild(rrFoot);

        // Tail
        Bone tail = new Bone("bone_tail", "TAIL");
        tail.getLocalTransform().setPosition(0f, 0f, -0.15f);
        pelvis.addChild(tail);

        return new Skeleton(root);
    }

    /**
     * Phase 9 Alignment: Builds bird creature skeleton topology with wing joints.
     */
    public static Skeleton buildBirdSkeleton() {
        Bone root = new Bone("bone_root", "ROOT");
        Bone spine = new Bone("bone_spine", "SPINE");
        spine.getLocalTransform().setPosition(0f, 0.5f, 0f);
        root.addChild(spine);

        Bone neck = new Bone("bone_neck", "NECK");
        neck.getLocalTransform().setPosition(0f, 0.15f, 0.1f);
        spine.addChild(neck);

        Bone head = new Bone("bone_head", "HEAD");
        head.getLocalTransform().setPosition(0f, 0.15f, 0.1f);
        neck.addChild(head);

        // Left Wing
        Bone lWingArm = new Bone("bone_l_wing_arm", "LEFT_WING_ARM");
        lWingArm.getLocalTransform().setPosition(-0.15f, 0.05f, 0f);
        spine.addChild(lWingArm);

        Bone lWingTip = new Bone("bone_l_wing_tip", "LEFT_WING_TIP");
        lWingTip.getLocalTransform().setPosition(-0.35f, 0f, 0f);
        lWingArm.addChild(lWingTip);

        // Right Wing
        Bone rWingArm = new Bone("bone_r_wing_arm", "RIGHT_WING_ARM");
        rWingArm.getLocalTransform().setPosition(0.15f, 0.05f, 0f);
        spine.addChild(rWingArm);

        Bone rWingTip = new Bone("bone_r_wing_tip", "RIGHT_WING_TIP");
        rWingTip.getLocalTransform().setPosition(0.35f, 0f, 0f);
        rWingArm.addChild(rWingTip);

        // Legs
        Bone lLeg = new Bone("bone_l_leg", "LEFT_LEG");
        lLeg.getLocalTransform().setPosition(-0.1f, -0.2f, 0f);
        spine.addChild(lLeg);

        Bone rLeg = new Bone("bone_r_leg", "RIGHT_LEG");
        rLeg.getLocalTransform().setPosition(0.1f, -0.2f, 0f);
        spine.addChild(rLeg);

        Bone tail = new Bone("bone_tail", "TAIL");
        tail.getLocalTransform().setPosition(0f, -0.05f, -0.2f);
        spine.addChild(tail);

        return new Skeleton(root);
    }
}