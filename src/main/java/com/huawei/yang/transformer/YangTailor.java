package com.huawei.yang.transformer;

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.*;
import org.yangcentral.yangkit.model.api.stmt.Module;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class YangTailor {
    private YangSchemaContext schemaContext;
    private YangTailorRule tailorRule;

    public YangTailor(YangSchemaContext schemaContext, YangTailorRule tailorRule) {
        this.schemaContext = schemaContext;
        this.tailorRule = tailorRule;
    }

    public YangSchemaContext getSchemaContext() {
        return schemaContext;
    }

    public YangTailorRule getTailorRule() {
        return tailorRule;
    }

    private boolean reserve(Module module){
        if(tailorRule.getTailorType()== TailorType.MODULE){
            if(module instanceof SubModule){
                return reserve(module.getMainModule());
            }
            List<String> matches = tailorRule.getMatches();
            boolean match = false;
            for(String pattern:matches){
                Pattern pat = Pattern.compile(pattern);
                if(pat.matcher(module.getArgStr()).lookingAt()){
                    match = true;
                    break;
                }
            }
            if(match){
                for(String invertPattern: getTailorRule().getNoMatches()){
                    Pattern pat = Pattern.compile(invertPattern);
                    if(pat.matcher(module.getArgStr()).lookingAt()){
                        match = false;
                        break;
                    }
                }
            }
            if(match){
                return true;
            }
            if(!module.getDependentBys().isEmpty()){
                for(Module dependent: module.getDependentBys()){
                    if(reserve(dependent)){
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    public void tailor(){
        Iterator<Map.Entry<String, List<YangElement>>> it = schemaContext.getParseResult().entrySet().iterator();
        List<String> tailoredModules = new ArrayList<>();
        while(it.hasNext()){
            Map.Entry<String, List<YangElement>> entry = it.next();
            List<YangElement> elements = entry.getValue();
            if(elements == null || elements.size() ==0){
                continue;
            }
            int size = elements.size();

            for(int i = 0; i < size;i++){
                YangElement element = elements.get(i);
                if(element instanceof YangStatement){
                    assert (element instanceof Module);
                    Module curModule = (Module) element;

                    if(!reserve(curModule)){
                        tailoredModules.add(entry.getKey());
                        schemaContext.removeModule(curModule.getModuleId());
                        for(Import im:curModule.getImports()){
                            if(im.getImport().isPresent()){
                                MainModule importedModule = im.getImport().get();
                                importedModule.removeDependentBy(curModule);
                            }
                        }
                    }
                }
            }
        }
        for(String tailoredModule:tailoredModules){
            schemaContext.getParseResult().remove(tailoredModule);
        }
    }
}
