package com.huawei.yang.transformer;

import com.huawei.yang.model.api.schema.ModuleId;
import com.huawei.yang.model.api.stmt.Module;

/**
 * 功能描述
 *
 * @author f00360218
 * @since 2022-05-18
 */
class LinkageInfo {
    private LinkageType type;
    private ModuleId moduleId;
    private String prefix;

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public LinkageInfo(LinkageType type, ModuleId moduleId) {
        this.type = type;
        this.moduleId = moduleId;
    }

    public LinkageType getType() {
        return type;
    }

    public ModuleId getModuleId() {
        return moduleId;
    }

    public String getPrefix() {
        return prefix;
    }

}
