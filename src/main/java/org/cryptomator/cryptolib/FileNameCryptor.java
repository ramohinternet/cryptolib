/*******************************************************************************
 * Copyright (c) 2015, 2016 Sebastian Stenzel and others.
 * This file is licensed under the terms of the MIT license.
 * See the LICENSE.txt file for more info.
 *
 * Contributors:
 *     Sebastian Stenzel - initial API and implementation
 *******************************************************************************/
package org.cryptomator.cryptolib;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.crypto.AEADBadTagException;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.codec.binary.BaseNCodec;
import org.cryptomator.siv.SivMode;

public class FileNameCryptor {

	private static final BaseNCodec BASE32 = new Base32();
	// private static final Pattern BASE32_PATTERN = Pattern.compile("([A-Z0-9]{8})*[A-Z0-9=]{8}");
	private static final ThreadLocal<SivMode> AES_SIV = new ThreadLocal<SivMode>() {
		@Override
		protected SivMode initialValue() {
			return new SivMode();
		};
	};

	private final SecretKey encryptionKey;
	private final SecretKey macKey;

	FileNameCryptor(SecretKey encryptionKey, SecretKey macKey) {
		this.encryptionKey = encryptionKey;
		this.macKey = macKey;
	}

	public String hashDirectoryId(String cleartextDirectoryId) {
		byte[] cleartextBytes = cleartextDirectoryId.getBytes(UTF_8);
		byte[] encryptedBytes = AES_SIV.get().encrypt(encryptionKey, macKey, cleartextBytes);
		byte[] hashedBytes = MessageDigestSupplier.SHA1.get().digest(encryptedBytes);
		return BASE32.encodeAsString(hashedBytes);
	}

	public String encryptFilename(String cleartextName, byte[]... associatedData) {
		byte[] cleartextBytes = cleartextName.getBytes(UTF_8);
		byte[] encryptedBytes = AES_SIV.get().encrypt(encryptionKey, macKey, cleartextBytes, associatedData);
		return BASE32.encodeAsString(encryptedBytes);
	}

	public String decryptFilename(String ciphertextName, byte[]... associatedData) throws AuthenticationFailedException {
		try {
			byte[] encryptedBytes = BASE32.decode(ciphertextName);
			byte[] cleartextBytes = AES_SIV.get().decrypt(encryptionKey, macKey, encryptedBytes, associatedData);
			return new String(cleartextBytes, UTF_8);
		} catch (AEADBadTagException e) {
			throw new AuthenticationFailedException("Authentication failed.", e);
		}
	}

}
