#include <jni.h>
#include <vulkan/vulkan.h>
#include <sstream>
#include <string>
#include <vector>

static std::string version_string(uint32_t v) {
    std::ostringstream out;
    out << VK_VERSION_MAJOR(v) << "."
        << VK_VERSION_MINOR(v) << "."
        << VK_VERSION_PATCH(v);
    return out.str();
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_eightcee_mk64recomp_NativeBridge_graphicsInfo(
        JNIEnv* env,
        jobject) {
    VkApplicationInfo app{};
    app.sType = VK_STRUCTURE_TYPE_APPLICATION_INFO;
    app.pApplicationName = "MK64Recomp";
    app.applicationVersion = VK_MAKE_VERSION(0, 1, 0);
    app.pEngineName = "MK64 Android Host";
    app.engineVersion = VK_MAKE_VERSION(0, 1, 0);
    app.apiVersion = VK_API_VERSION_1_0;

    VkInstanceCreateInfo create{};
    create.sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
    create.pApplicationInfo = &app;

    VkInstance instance = VK_NULL_HANDLE;
    VkResult result = vkCreateInstance(&create, nullptr, &instance);
    if (result != VK_SUCCESS) {
        std::string text = "Vulkan unavailable: vkCreateInstance=" + std::to_string(result);
        return env->NewStringUTF(text.c_str());
    }

    uint32_t count = 0;
    result = vkEnumeratePhysicalDevices(instance, &count, nullptr);
    if (result != VK_SUCCESS || count == 0) {
        vkDestroyInstance(instance, nullptr);
        std::string text = "Vulkan instance OK; no physical device result=" +
            std::to_string(result);
        return env->NewStringUTF(text.c_str());
    }

    std::vector<VkPhysicalDevice> devices(count);
    vkEnumeratePhysicalDevices(instance, &count, devices.data());

    std::ostringstream out;
    out << "Vulkan devices=" << count;

    for (uint32_t i = 0; i < count; ++i) {
        VkPhysicalDeviceProperties props{};
        vkGetPhysicalDeviceProperties(devices[i], &props);
        out << " [" << i << "] "
            << props.deviceName
            << " api=" << version_string(props.apiVersion)
            << " driver=" << props.driverVersion
            << " vendor=0x" << std::hex << props.vendorID
            << " device=0x" << props.deviceID << std::dec;
    }

    vkDestroyInstance(instance, nullptr);
    const std::string text = out.str();
    return env->NewStringUTF(text.c_str());
}
