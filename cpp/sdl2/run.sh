g++ -std=c++11 -o sdl_interface ubuntu.cpp -lSDL2 -lpthread


arm-linux-gnueabihf-g++ -DDIRECTFB -std=c++11 -O3 -fno-strict-aliasing -fPIC -marm -mtune=cortex-a7 -march=armv7ve+simd -mfpu=neon-vfpv4 -mfloat-abi=hard -lSDL2 -lpthread  miyoomini.cpp cJSON.c -o sdl_interface


aarch64-none-linux-gnu-g++ -std=c++11  -fPIC -O3 -fno-strict-aliasing -march=armv8-a+crc -mtune=cortex-a53 -lSDL2 -lpthread  gkdminiplus.cpp cJSON.c -o sdl_interface
