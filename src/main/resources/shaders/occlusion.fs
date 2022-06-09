#version 430
layout(early_fragment_tests) in;

flat in int id;

writeonly layout(std430) buffer occlusionBuffer {
    int[] occlusion;
};


void main() {
    /*
    Assumes incoherent 32-bit write-only is safe.
    If the native word size happens to be larger, this could be racy.
    */

    occlusion[id] = 1;
}