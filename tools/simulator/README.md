# Maverick AI Simulator

The Maverick AI Simulator emulates the glasses display via a web dashboard, allowing UI development without wearing the glasses.

The SDK connects to the simulator over **WebSocket** on the local network.

Download instructions and usage information can be found in our [developers portal](https://everysight.github.io/maverick-ai-docs/guides/simulator/)

## Limitations

- Sensors are simulated, not physical: ambient light and the inertial stream are driven from
  the dashboard. Proximity is not simulated.
- No firmware update support (OTA)
- The AIVision sensor and the microphone use the PC's devices (first camera found, default mic)

