package kr.lastdish.core.common.crypto;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;

@Converter(autoApply = false)
public class EncryptConverter implements AttributeConverter<String, String> {
  // AES/GCM 알고리즘 사용
  private static final String TRANSFORM = "AES/GCM/NoPadding";
  private static final int IV_LENGTH = 12;
  private static final int TAG_LENGTH = 128;

  @Value("${encryption.secret-key}")
  private String secretKey; // 32자(256bit)

  private SecretKeySpec secretKeySpec;

  @PostConstruct
  public void init() {
    this.secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
  }

  // 평문 -> 암호문
  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) return null;
    try {
      byte[] iv = new byte[IV_LENGTH];
      new SecureRandom().nextBytes(iv); // 랜덤 IV 생성. 암호화 할 때마다 결과 달라짐

      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));

      // 암호화 실행 부분
      byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));

      byte[] result =
          ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
      // DB에 문자열로 저장
      return Base64.getEncoder().encodeToString(result);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("암호화 실패", e);
    }
  }

  // 암호문 -> 평문
  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) return null;
    try {
      // 문자열을 디코딩해서 바이트 배열 [IV + 암호문]로 복원
      byte[] decoded = Base64.getDecoder().decode(dbData);
      ByteBuffer buffer = ByteBuffer.wrap(decoded);

      byte[] iv = new byte[IV_LENGTH];
      buffer.get(iv);
      byte[] encrypted = new byte[buffer.remaining()];
      buffer.get(encrypted);

      Cipher cipher = Cipher.getInstance(TRANSFORM);
      cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH, iv));

      return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("복호화 실패", e);
    }
  }
}
