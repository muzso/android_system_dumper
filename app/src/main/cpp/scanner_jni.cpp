#include <jni.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <string>
#include <vector>
#include <cstring>

extern "C" {

JNIEXPORT jobjectArray JNICALL
Java_hu_muzso_android_1system_1dumper_platform_JniNativeBridge_listDirectoryNative(JNIEnv *env, jobject thiz,
                                                                        jstring path_obj, jobject job_obj) {
    if (path_obj == nullptr) {
        jclass entryClass = env->FindClass("hu/muzso/android_system_dumper/platform/JniNativeBridge$NativeDirEntry");
        return env->NewObjectArray(0, entryClass, nullptr);
    }

    const char *pathStr = env->GetStringUTFChars(path_obj, nullptr);
    if (pathStr == nullptr) {
        jclass entryClass = env->FindClass("hu/muzso/android_system_dumper/platform/JniNativeBridge$NativeDirEntry");
        return env->NewObjectArray(0, entryClass, nullptr);
    }

    DIR *dir = opendir(pathStr);
    if (dir == nullptr) {
        env->ReleaseStringUTFChars(path_obj, pathStr);
        jclass entryClass = env->FindClass("hu/muzso/android_system_dumper/platform/JniNativeBridge$NativeDirEntry");
        return env->NewObjectArray(0, entryClass, nullptr);
    }

    jmethodID isActiveMethod = nullptr;
    if (job_obj != nullptr) {
        jclass jobClass = env->GetObjectClass(job_obj);
        isActiveMethod = env->GetMethodID(jobClass, "isActive", "()Z");
    }

    std::vector<std::pair<std::string, int>> entries;
    struct dirent *entry;

    while ((entry = readdir(dir)) != nullptr) {
        // Cooperative cancellation check between readdir calls
        if (isActiveMethod != nullptr) {
            jboolean isActive = env->CallBooleanMethod(job_obj, isActiveMethod);
            if (!isActive) {
                break;
            }
        }

        const char *name = entry->d_name;
        // Filter out "." and ".."
        if (strcmp(name, ".") == 0 || strcmp(name, "..") == 0) {
            continue;
        }

        int type = entry->d_type;

        // Resolve DT_UNKNOWN (0) using lstat
        if (type == 0) { // DT_UNKNOWN
            std::string fullPath = std::string(pathStr);
            if (fullPath.empty() || fullPath.back() != '/') {
                fullPath += "/";
            }
            fullPath += name;

            struct stat st;
            if (lstat(fullPath.c_str(), &st) == 0) {
                if (S_ISREG(st.st_mode)) {
                    type = 8; // DT_REG
                } else if (S_ISDIR(st.st_mode)) {
                    type = 4; // DT_DIR
                } else if (S_ISLNK(st.st_mode)) {
                    type = 10; // DT_LNK
                }
            }
        }

        entries.push_back({std::string(name), type});
    }

    closedir(dir);
    env->ReleaseStringUTFChars(path_obj, pathStr);

    jclass entryClass = env->FindClass("hu/muzso/android_system_dumper/platform/JniNativeBridge$NativeDirEntry");
    if (entryClass == nullptr) {
        return nullptr;
    }

    jmethodID constructor = env->GetMethodID(entryClass, "<init>", "(Ljava/lang/String;I)V");
    if (constructor == nullptr) {
        return nullptr;
    }

    jobjectArray resultArr = env->NewObjectArray(entries.size(), entryClass, nullptr);
    if (resultArr == nullptr) {
        return nullptr;
    }

    for (size_t i = 0; i < entries.size(); ++i) {
        jstring nameStr = env->NewStringUTF(entries[i].first.c_str());
        jobject entryObj = env->NewObject(entryClass, constructor, nameStr, entries[i].second);
        env->SetObjectArrayElement(resultArr, i, entryObj);
        env->DeleteLocalRef(nameStr);
        env->DeleteLocalRef(entryObj);
    }

    return resultArr;
}

}
