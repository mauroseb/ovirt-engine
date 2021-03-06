package org.ovirt.engine.core.common.businessentities;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum BiosType implements Identifiable {

    I440FX_SEA_BIOS(0, ChipsetType.I440FX, false),
    Q35_SEA_BIOS(1, ChipsetType.Q35, false),
    Q35_OVMF(2, ChipsetType.Q35, true),
    Q35_SECURE_BOOT(3, ChipsetType.Q35, true);

    private int value;
    private ChipsetType chipsetType;
    private boolean ovmf;

    private static final Map<Integer, BiosType> valueToBios =
            Stream.of(values()).collect(Collectors.toMap(BiosType::getValue, Function.identity()));

    BiosType(int value, ChipsetType chipsetType, boolean ovmf) {
        this.value = value;
        this.chipsetType = chipsetType;
        this.ovmf = ovmf;
    }

    public int getValue() {
        return value;
    }

    public ChipsetType getChipsetType() {
        return chipsetType;
    }

    public boolean isOvmf() {
        return ovmf;
    }

    public static BiosType forValue(int value) {
        return valueToBios.get(value);
    }

}
