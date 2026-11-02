#include <jni.h>

#include <string>

#include "gtn_native_core.h"

extern "C" JNIEXPORT jlong JNICALL
Java_com_jarrlyyy_guessthenumber_shared_engine_NativeGameplayOptimizer_nativeComputeAdaptiveTarget(
    JNIEnv* env,
    jobject,
    jlong rangeMin,
    jlong rangeMax,
    jstring difficulty,
    jint streak,
    jlong attempts,
    jlong seed) {
    const char* chars = difficulty == nullptr ? nullptr : env->GetStringUTFChars(difficulty, nullptr);
    const std::string difficultyId = chars == nullptr ? "classic" : std::string(chars);
    if (chars != nullptr) env->ReleaseStringUTFChars(difficulty, chars);
    return static_cast<jlong>(gtn_native::adaptiveTarget(
        static_cast<int64_t>(rangeMin),
        static_cast<int64_t>(rangeMax),
        difficultyId,
        static_cast<int32_t>(streak),
        static_cast<int64_t>(attempts),
        static_cast<int64_t>(seed)));
}


#include <jni.h>

#include <array>
#include <cstdint>
#include <limits>
#include <string>
#include <vector>

#include "mbedtls/gcm.h"
#include "mbedtls/platform_util.h"
#include "mbedtls/sha256.h"

namespace {

constexpr char kArchiveMagic[] = "GTNGBIN1";
constexpr std::uint8_t kArchiveFormatVersion = 1;
constexpr std::size_t kHeaderSize = 21;
constexpr std::size_t kNonceSize = 12;
constexpr std::size_t kTagSize = 16;
constexpr std::size_t kKeySize = 32;
constexpr jsize kMaxChunkBytes = 16 * 1024 * 1024;
constexpr char kKeyMaterial[] = "GuessTheNumber|game-data|archive-v1|2026";

void throwJava(JNIEnv* env, const char* className, const char* message) {
    if (env->ExceptionCheck()) return;
    jclass exceptionClass = env->FindClass(className);
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message);
        env->DeleteLocalRef(exceptionClass);
    }
}

bool validGroup(const std::string& value) {
    if (value.empty() || value.size() > 40) return false;
    for (const char character : value) {
        if ((character < 'a' || character > 'z') && character != '-') return false;
    }
    return true;
}

bool validChunkId(const std::string& value) {
    if (value.size() < 2 || value.size() > 4) return false;
    for (const char character : value) {
        if (character < '0' || character > '9') return false;
    }
    return true;
}

class ScopedUtfChars {
public:
    ScopedUtfChars(JNIEnv* env, jstring value) : env_(env), value_(value) {
        if (value_ != nullptr) chars_ = env_->GetStringUTFChars(value_, nullptr);
    }

    ~ScopedUtfChars() {
        if (chars_ != nullptr) env_->ReleaseStringUTFChars(value_, chars_);
    }

    const char* get() const { return chars_; }

private:
    JNIEnv* env_;
    jstring value_;
    const char* chars_ = nullptr;
};

}  // namespace

extern "C" JNIEXPORT jbyteArray JNICALL
Java_com_jarrlyyy_guessthenumber_data_repository_NativeGameDataCrypto_nativeDecryptChunk(
    JNIEnv* env,
    jobject,
    jbyteArray encryptedChunk,
    jstring groupValue,
    jstring chunkIdValue) {
    if (encryptedChunk == nullptr || groupValue == nullptr || chunkIdValue == nullptr) {
        throwJava(env, "java/lang/IllegalArgumentException", "Game-data chunk arguments must not be null");
        return nullptr;
    }

    const jsize blobLength = env->GetArrayLength(encryptedChunk);
    if (blobLength < static_cast<jsize>(kHeaderSize + kTagSize) ||
        blobLength > kMaxChunkBytes) {
        throwJava(env, "java/lang/SecurityException", "Game-data chunk length is invalid");
        return nullptr;
    }

    ScopedUtfChars groupChars(env, groupValue);
    ScopedUtfChars idChars(env, chunkIdValue);
    if (groupChars.get() == nullptr || idChars.get() == nullptr) return nullptr;

    const std::string group(groupChars.get());
    const std::string chunkId(idChars.get());
    if (!validGroup(group) || !validChunkId(chunkId)) {
        throwJava(env, "java/lang/SecurityException", "Game-data chunk context is invalid");
        return nullptr;
    }

    std::vector<std::uint8_t> blob(static_cast<std::size_t>(blobLength));
    env->GetByteArrayRegion(encryptedChunk, 0, blobLength, reinterpret_cast<jbyte*>(blob.data()));
    if (env->ExceptionCheck()) return nullptr;

    if (std::string(reinterpret_cast<const char*>(blob.data()), sizeof(kArchiveMagic) - 1) != kArchiveMagic ||
        blob[8] != kArchiveFormatVersion) {
        throwJava(env, "java/lang/SecurityException", "Game-data archive header is invalid");
        return nullptr;
    }

    const std::size_t sealedLength = blob.size() - kHeaderSize;
    if (sealedLength <= kTagSize) {
        throwJava(env, "java/lang/SecurityException", "Game-data ciphertext is empty");
        return nullptr;
    }
    const std::size_t ciphertextLength = sealedLength - kTagSize;

    std::array<unsigned char, kKeySize> key{};
    if (mbedtls_sha256(
            reinterpret_cast<const unsigned char*>(kKeyMaterial),
            sizeof(kKeyMaterial) - 1,
            key.data(),
            0) != 0) {
        mbedtls_platform_zeroize(key.data(), key.size());
        throwJava(env, "java/lang/SecurityException", "Could not derive game-data archive key");
        return nullptr;
    }

    mbedtls_gcm_context context;
    mbedtls_gcm_init(&context);
    const int setKeyResult = mbedtls_gcm_setkey(
        &context, MBEDTLS_CIPHER_ID_AES, key.data(), 256);
    mbedtls_platform_zeroize(key.data(), key.size());
    if (setKeyResult != 0) {
        mbedtls_gcm_free(&context);
        throwJava(env, "java/lang/SecurityException", "Could not initialize AES-256-GCM");
        return nullptr;
    }

    const std::string associatedData = "1:" + group + ":" + chunkId;
    std::vector<unsigned char> plaintext(ciphertextLength);
    const unsigned char* nonce = blob.data() + 9;
    const unsigned char* ciphertext = blob.data() + kHeaderSize;
    const unsigned char* tag = ciphertext + ciphertextLength;

    const int decryptResult = mbedtls_gcm_auth_decrypt(
        &context,
        ciphertextLength,
        nonce,
        kNonceSize,
        reinterpret_cast<const unsigned char*>(associatedData.data()),
        associatedData.size(),
        tag,
        kTagSize,
        ciphertext,
        plaintext.data());
    mbedtls_gcm_free(&context);

    if (decryptResult != 0) {
        if (!plaintext.empty()) mbedtls_platform_zeroize(plaintext.data(), plaintext.size());
        throwJava(env, "java/lang/SecurityException", "Game-data AES-GCM authentication failed");
        return nullptr;
    }

    if (plaintext.size() > static_cast<std::size_t>(std::numeric_limits<jsize>::max())) {
        mbedtls_platform_zeroize(plaintext.data(), plaintext.size());
        throwJava(env, "java/lang/SecurityException", "Decrypted game-data chunk is too large");
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(static_cast<jsize>(plaintext.size()));
    if (result != nullptr) {
        env->SetByteArrayRegion(
            result, 0, static_cast<jsize>(plaintext.size()),
            reinterpret_cast<const jbyte*>(plaintext.data()));
    }
    if (!plaintext.empty()) mbedtls_platform_zeroize(plaintext.data(), plaintext.size());
    return result;
}
