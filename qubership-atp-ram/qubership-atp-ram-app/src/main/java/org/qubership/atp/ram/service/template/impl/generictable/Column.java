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

package org.qubership.atp.ram.service.template.impl.generictable;

import static java.util.Objects.isNull;

import java.util.ArrayList;
import java.util.List;

import org.qubership.atp.ram.models.Label;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Column {

    public static final String BLACK = "#000000";
    public static final String GREEN = "#00BB5B";
    public static final String ORANGE = "#FFB02E";
    public static final String RED = "#FF5260";
    public static final String TRANSPARENT_DARK_RED = "#F7BCC3";
    public static final String TRANSPARENT_LIGHT_RED = "#FFC2C7";
    public static final String GRAY = "#8F9EB4";
    public static final String BOLD = "bold";
    public static final String N_A = "N/A";
    public static final String DASH = "—";
    public static final String EMPTY = " ";

    private String type;
    private String fontWeight;
    private String color;
    private String backgroundColor;
    private String alternativeBackgroundColor;
    private String text;
    private String url;
    private String textSuffix;
    private List<Link> links = new ArrayList<>();
    private List<Label> labels = new ArrayList<>();

    public Column(Object text) {
        this(ColumnType.TEXT, isNull(text) ? Column.EMPTY : text.toString(), null, null, null, null, null, null);
    }

    public Column(String text, String fontWeight) {
        this(ColumnType.TEXT, text, fontWeight, null, null, null, null, null);
    }

    public Column(String text, String fontWeight, String color) {
        this(ColumnType.TEXT, text, fontWeight, color, null, null, null, null);
    }

    /**
     * Column constructor.
     */
    public Column(ColumnType type, String text, String fontWeight, String color,
                  String url, List<Link> links, String textSuffix, List<Label> labels) {
        this.text = text;
        this.fontWeight = fontWeight;
        this.color = color;
        this.type = type.getType();
        this.url = url;
        this.links = links;
        this.textSuffix = textSuffix;
        this.labels = labels;
    }

    @Getter
    @AllArgsConstructor
    public enum ColumnType {
        TEXT("text"),
        LINK("link"),
        LINK_LIST("linkList"),
        LABEL_LIST("labelList");

        private String type;
    }
}
