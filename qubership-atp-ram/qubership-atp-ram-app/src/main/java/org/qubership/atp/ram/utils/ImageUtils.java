/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.ram.utils;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Deprecated
@UtilityClass
@Slf4j
public class ImageUtils {

    /**
     * Returns a resized image in an encrypted string in base64.
     * @param content Image {@link InputStream}.
     * @param logRecordId LogRecord Id.
     * @return Encrypted string in base64.
     */
    public String scaleAndConvertImageToBase64(InputStream content, UUID logRecordId) {
        try {
            final int width = 300;
            final String ext = "jpg";
            BufferedImage image = ImageIO.read(content);
            BufferedImage resized = resize(image, width, width * image.getHeight() / image.getWidth());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(resized, ext, outputStream);

            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (IOException e) {
            log.error("Failed to scale nad convert preview for log record with id: {}", logRecordId, e);
        }

        return "";
    }

    private BufferedImage resize(BufferedImage img, int width, int height) {
        Image tmp = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return resized;
    }
}
