package com.domerry.config;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.encryption.pbe.PooledPBEStringEncryptor;
import org.jasypt.encryption.pbe.config.SimpleStringPBEConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 自定义加密器。
 *
 * <p>算法由 PBEWITHMD5ANDDES（MD5 + DES，均已不安全）升级为
 * PBEWITHHMACSHA512ANDAES_256 + 随机 IV：AES-256 加密、HMAC-SHA512 认证、PBKDF2 派生密钥，
 * 同一明文每次加密结果不同。
 *
 * <p><b>迁移期兼容</b>：{@link #encrypt} 一律使用强算法；{@link #decrypt} 先用强算法尝试，
 * 失败才回退旧算法并打印告警，使存量 {@code ENC(...)} 在重新加密前仍可启动。
 * 全部密文迁移完成后应删除 {@link #legacyEncryptor} 回退分支。
 * 迁移步骤见 docs/安全方案-密钥泄露处置与环境变量配置.md。
 */
public class MyStringEncryptor implements StringEncryptor {

    private static final Logger log = LoggerFactory.getLogger(MyStringEncryptor.class);

    /**
     * 运行时算法：AES-256 + HMAC-SHA512
     */
    public static final String STRONG_ALGORITHM = "PBEWITHHMACSHA512ANDAES_256";

    /**
     * 历史算法：仅用于迁移期解密存量密文，禁止用于新加密
     */
    public static final String LEGACY_ALGORITHM = "PBEWITHMD5ANDDES";

    /**
     * 强算法密钥派生迭代次数
     */
    public static final int DEFAULT_ITERATIONS = 10000;

    /**
     * 旧算法迭代次数（与历史密文保持一致，改动会导致旧密文解不开）
     */
    private static final int LEGACY_ITERATIONS = 1000;

    private static final String IV_RANDOM = "org.jasypt.iv.RandomIvGenerator";
    private static final String IV_NONE = "org.jasypt.iv.NoIvGenerator";

    private final String password;

    private final PooledPBEStringEncryptor strongEncryptor;

    /**
     * 旧算法加密器：仅在实际解到旧密文时创建，避免正常路径承担额外开销
     */
    private volatile PooledPBEStringEncryptor legacyEncryptor;

    public MyStringEncryptor(String password) {
        this(password, DEFAULT_ITERATIONS);
    }

    public MyStringEncryptor(String password, int keyObtentionIterations) {
        this.password = password;
        this.strongEncryptor = build(password, STRONG_ALGORITHM, keyObtentionIterations, IV_RANDOM);
    }

    @Override
    public String encrypt(String message) {
        return strongEncryptor.encrypt(message);
    }

    @Override
    public String decrypt(String encryptedMessage) {
        try {
            return strongEncryptor.decrypt(encryptedMessage);
        } catch (Exception strongFailure) {
            try {
                String plain = legacyEncryptor().decrypt(encryptedMessage);
                log.warn("检测到旧算法({})密文，已用回退分支解密。请尽快用 {} 重新加密并轮换盐值，"
                        + "迁移步骤见 docs/安全方案-密钥泄露处置与环境变量配置.md", LEGACY_ALGORITHM, STRONG_ALGORITHM);
                return plain;
            } catch (Exception legacyFailure) {
                throw new IllegalStateException("ENC 密文解密失败：密文与当前盐值/算法不匹配。"
                        + "若盐值已轮换，需用新盐值以 " + STRONG_ALGORITHM + " 重新加密全部 ENC(...)；"
                        + "迁移步骤见 docs/安全方案-密钥泄露处置与环境变量配置.md", strongFailure);
            }
        }
    }

    private PooledPBEStringEncryptor legacyEncryptor() {
        PooledPBEStringEncryptor local = legacyEncryptor;
        if (local == null) {
            synchronized (this) {
                local = legacyEncryptor;
                if (local == null) {
                    local = build(password, LEGACY_ALGORITHM, LEGACY_ITERATIONS, IV_NONE);
                    legacyEncryptor = local;
                }
            }
        }
        return local;
    }

    private static PooledPBEStringEncryptor build(String password, String algorithm,
                                                  int iterations, String ivGenerator) {
        PooledPBEStringEncryptor encryptor = new PooledPBEStringEncryptor();
        SimpleStringPBEConfig config = new SimpleStringPBEConfig();
        // 加解密盐值
        config.setPassword(password);
        // 加解密算法
        config.setAlgorithm(algorithm);
        // 密钥派生迭代次数
        config.setKeyObtentionIterations(iterations);
        // 线程池大小：默认1
        config.setPoolSize(1);
        // 盐值生成器
        config.setSaltGeneratorClassName("org.jasypt.salt.RandomSaltGenerator");
        // AES-256 必须配合随机 IV；旧 MD5DES 密文使用 NoIvGenerator
        config.setIvGeneratorClassName(ivGenerator);
        // 字符串输出类型
        config.setStringOutputType("base64");
        encryptor.setConfig(config);
        return encryptor;
    }
}
