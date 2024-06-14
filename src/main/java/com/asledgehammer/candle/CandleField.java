package com.asledgehammer.candle;

import com.asledgehammer.rosetta.RosettaClass;
import com.asledgehammer.rosetta.RosettaField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;

public class CandleField extends CandleEntity<CandleField> {

    private final boolean bPublic;
    private final boolean bProtected;
    private final boolean bPrivate;
    private final boolean bStatic;
    private final boolean bFinal;

    @NotNull
    private final String name;

    @NotNull
    private final Field field;
    private final CandleClass candleClass;

    @Nullable
    private RosettaField docs;

    CandleField(@NotNull CandleClass candleClass, @NotNull Field field) {
        super(field.getType(), field.getName());

        this.candleClass = candleClass;

        int modifiers = field.getModifiers();
        this.bStatic = Modifier.isStatic(modifiers);
        this.bPublic = Modifier.isPublic(modifiers);
        this.bProtected = Modifier.isProtected(modifiers);
        this.bPrivate = Modifier.isPrivate(modifiers);
        this.bFinal = Modifier.isFinal(modifiers);
        this.name = field.getName();
        this.field = field;
    }

    @Override
    void onWalk(@NotNull CandleGraph graph) {
        RosettaClass yamlFile = candleClass.getDocs();
        if (yamlFile != null) {
            docs = yamlFile.getField(field.getName());
        }
    }

    public String getName() {
        return name;
    }

    public boolean isPublic() {
        return bPublic;
    }

    public boolean isProtected() {
        return bProtected;
    }

    public boolean isPrivate() {
        return bPrivate;
    }

    public boolean isFinal() {
        return bFinal;
    }

    public boolean isStatic() {
        return bStatic;
    }

    public RosettaField getDocs() {
        return this.docs;
    }

    public boolean hasDocs() {
        return this.docs != null;
    }

    public String getBasicType() {
        StringBuilder basicType = new StringBuilder(this.field.getType().getSimpleName());
        ParameterizedType stringListType = (ParameterizedType) this.field.getGenericType();
        Class<?>[] genericArgs = (Class<?>[]) stringListType.getActualTypeArguments();
        if (genericArgs.length != 0) {
            basicType.append('<');
            for (Class<?> genericArg : genericArgs) {
                basicType.append(genericArg.getSimpleName()).append(", ");
            }
            basicType = new StringBuilder(basicType.substring(0, basicType.length() - 2) + '>');
        }
        return basicType.toString();
    }

    public String getFullType() {
        StringBuilder basicType = new StringBuilder(this.field.getType().getName());
        ParameterizedType stringListType = (ParameterizedType) this.field.getGenericType();
        Class<?>[] genericArgs = (Class<?>[]) stringListType.getActualTypeArguments();
        if (genericArgs.length != 0) {
            basicType.append('<');
            for (Class<?> genericArg : genericArgs) {
                basicType.append(genericArg.getName()).append(", ");
            }
            basicType = new StringBuilder(basicType.substring(0, basicType.length() - 2) + '>');
        }
        return basicType.toString();
    }
}
