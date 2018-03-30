package com.github.zxh.classpy.classfile.datatype;

import com.github.zxh.classpy.classfile.ClassFileComponent;
import com.github.zxh.classpy.classfile.ClassFileReader;
import com.github.zxh.classpy.common.ParseException;
import com.github.zxh.classpy.classfile.attribute.AttributeFactory;
import com.github.zxh.classpy.classfile.constant.ConstantPool;
import com.github.zxh.classpy.classfile.attribute.AttributeInfo;
import com.github.zxh.classpy.common.FileComponent;
import com.github.zxh.classpy.helper.StringHelper;

/**
 * Array of class components.
 */
public class Table extends ClassFileComponent {

    private final UInt length;
    private final Class<? extends ClassFileComponent> entryClass;

    public Table(UInt length, Class<? extends ClassFileComponent> entryClass) {
        this.length = length;
        this.entryClass = entryClass;
    }
    
    @Override
    protected void readContent(ClassFileReader reader) {
        try {
            for (int i = 0; i < length.getValue(); i++) {
                super.add(readEntry(reader));
            }
        } catch (ReflectiveOperationException e) {
            throw new ParseException(e);
        }
    }

    private ClassFileComponent readEntry(ClassFileReader reader) throws ReflectiveOperationException {
        if (entryClass == AttributeInfo.class) {
            return readAttributeInfo(reader);
        } else {
            ClassFileComponent c = entryClass.newInstance();
            c.read(reader);
            return c;
        }
    }
    
    private AttributeInfo readAttributeInfo(ClassFileReader reader) {
        int attrNameIndex = reader.getShort(reader.getPosition());
        String attrName = reader.getConstantPool().getUtf8String(attrNameIndex);
        
        AttributeInfo attr = AttributeFactory.create(attrName);
        attr.setName(attrName);
        attr.read(reader);
        
        return attr;
    }

    @Override
    protected void postRead(ConstantPool cp) {
        int i = 0;
        for (FileComponent entry : super.getComponents()) {
            String newName = StringHelper.formatIndex(length.getValue(), i++);
            String oldName = entry.getName();
            if (oldName != null) {
                newName += " (" + oldName + ")";
            }
            entry.setName(newName);
        }
    }

}
