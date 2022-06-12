#version 430
layout(early_fragment_tests) in;

flat in int ID;

layout(std430) writeonly buffer occlusionBuffer {
    int[] occlusion;
};

uniform int frame;

void main() {
    /*
    Assumes incoherent 32-bit write-only is safe.
    If the native word size happens to be larger, this could be racy.
    */

    occlusion[0] = frame;
    occlusion[ID] = frame; //Swapping these writes omits this one - WTF?!
}