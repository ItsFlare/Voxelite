# Voxelite

**Voxelite** was a student project for professor Dachsbacher's computer graphics lab at the Karlsruhe Institute of Technology (Computer Science M.Sc.).

It consists of a proposal, 4 sets of presentation slides, 8k lines of source code written in 3 months and was awarded the maximum grade.

## Goals

- Implement contemporary rendering techniques
    - Volumetric light scattering as post-process
    - Cascaded shadow mapping (PCSS)
    - Screen-space reflections
    - Ambient occlusion
    - Normal mapping
    - Bloom
- Create an efficient OpenGL voxel renderer
    - Asynchronous chunk meshing
    - Six total culling methods
    - Texture atlas generation
    - Asynchronous transparency sorting
- Generate procedural terrain
    - Parallel noise-based chunk generator
    - Includes a few different biomes and structures
    - Practically unlimited world size
- RGB flood-fill voxel lighting

If you're thinking of a Minecraft clone - yes, pretty much that. We even stole the textures! Hopefully made up for it
with all our enhancements ;-)

### Non-goals

- Add gameplay elements beyond block placement
- Create a well-documented, future-proof codebase
- Cross-platform compatibility (Windows only)

## Dependencies

| Type                | Solution     |
| ------------------- | ------------- |
| Native bindings     | LWJGL         |
| OpenGL abstraction  | BeaconGL      |
| User Interface      | ImGui         |
| 3D Noise            | OpenSimplex2  |
| Logging             | Log4j         |
| Data Structures     | FastUtil      |
| Testing             | JUnit / JMH   |
| Other               | Gson / Jansi  |
