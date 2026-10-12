package com.domerry.utils;


import com.domerry.config.MyStringEncryptor;

/**
 * ENC 密文生成/校验工具。
 *
 * <p>算法固定为 {@link MyStringEncryptor#STRONG_ALGORITHM}（AES-256 + HMAC-SHA512），
 * 与运行时 {@link MyStringEncryptor} 保持一致，避免"生成用一个算法、解密用另一个"的历史问题。
 *
 * <p>盐值经环境变量 {@code JASYPT_ENCRYPTOR_PASSWORD} 注入，禁止硬编码到源码或配置文件。
 *
 * <p>用法（在 springboot-file 目录下执行，盐值走环境变量，避免进入 shell 历史）：
 * <pre>
 *   read -rs JASYPT_ENCRYPTOR_PASSWORD &amp;&amp; export JASYPT_ENCRYPTOR_PASSWORD
 *   mvn -q exec:java -Dexec.mainClass=com.chen.utils.JasyptUtil \
 *       -Dexec.args='encrypt 待加密的明文'
 * </pre>
 */
public class JasyptUtil {

    private JasyptUtil() {
    }

    /**
     * 用强算法加密
     *
     * @param plainText 明文
     * @param password  盐值
     */
    public static String encrypt(String plainText, String password) {
        return new MyStringEncryptor(password).encrypt(plainText);
    }

    /**
     * 解密（兼容旧算法密文，便于校验迁移结果）
     *
     * @param cipherText 密文（不含 ENC() 包裹）
     * @param password   盐值
     */
    public static String decrypt(String cipherText, String password) {
        return new MyStringEncryptor(password).decrypt(cipherText);
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("用法: JasyptUtil <encrypt|decrypt> <明文|密文>");
            System.err.println("盐值经环境变量 JASYPT_ENCRYPTOR_PASSWORD 注入");
            System.exit(64);
        }
        String password = System.getenv("JASYPT_ENCRYPTOR_PASSWORD");
        if (password == null || password.trim().isEmpty()) {
            System.err.println("缺少环境变量 JASYPT_ENCRYPTOR_PASSWORD");
            System.exit(64);
        }
        String mode = args[0];
        String input = args[1];
        if ("encrypt".equals(mode)) {
            // 只输出 ENC() 包裹后的密文，便于直接粘贴进 yml
            System.out.println("ENC(" + encrypt(input, password) + ")");
        } else if ("decrypt".equals(mode)) {
            String cipher = input.startsWith("ENC(") && input.endsWith(")")
                    ? input.substring(4, input.length() - 1) : input;
            System.out.println(decrypt(cipher, password));
        } else {
            System.err.println("未知模式：" + mode + "（支持 encrypt / decrypt）");
            System.exit(64);
        }
    }
}
