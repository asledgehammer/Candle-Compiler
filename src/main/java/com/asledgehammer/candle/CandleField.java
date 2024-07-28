package com.asledgehammer.candle;

import com.asledgehammer.rosetta.Rosetta;
import com.asledgehammer.rosetta.RosettaClass;
import com.asledgehammer.rosetta.RosettaField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public String getBasicType() {
        return CandleUtils.asBasicType(CandleUtils.getFullType(this.field));
    }

    public String getFullType() {
        return CandleUtils.getFullType(this.field);
    }

    @NotNull
    public RosettaField getDocs() {
        if (this.docs == null) {
            this.docs = new RosettaField(this.getLuaName(), this.genDocs());
        }
        return this.docs;
    }

    public boolean hasDocs() {
        return this.docs != null;
    }

    public boolean isDocsValid() {
        if (this.docs == null) return false;
        if (this.docs.getType().hasFull()) {
            return this.docs.getType().matches(this.getFullType(), this.getBasicType());
        } else {
            return this.docs.getType().matches(this.getBasicType());
        }
    }

    @NotNull
    public Map<String, Object> genDocs() {

//        System.out.println("Field.genDocs(): " + getLuaName());

        Map<String, Object> mapField = new HashMap<>();

        // MODIFIERS
        List<String> listModifiers = new ArrayList<>();
        if (this.isPublic()) listModifiers.add("public");
        else if (this.isProtected()) listModifiers.add("protected");
        else if (this.isPrivate()) listModifiers.add("private");
        if (this.isStatic()) listModifiers.add("static");
        if (this.isFinal()) listModifiers.add("final");
        mapField.put("modifiers", listModifiers);

        // TYPE
        Map<String, Object> mapType = new HashMap<>();
        mapType.put("basic", this.getBasicType());
        mapType.put("full", this.getFullType());
        mapType.put("nullable", true);
        mapField.put("type", mapType);

        return mapField;
    }
}
