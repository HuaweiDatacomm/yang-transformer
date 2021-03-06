package com.huawei.yang.transformer;

import org.yangcentral.yangkit.base.YangBuiltinKeyword;
import org.yangcentral.yangkit.base.YangContext;
import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.common.api.FName;
import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.common.api.validate.ValidatorResult;
import org.yangcentral.yangkit.model.api.restriction.LeafRef;
import org.yangcentral.yangkit.model.api.schema.ModuleId;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.Augment;
import org.yangcentral.yangkit.model.api.stmt.Case;
import org.yangcentral.yangkit.model.api.stmt.DataNodeModifier;
import org.yangcentral.yangkit.model.api.stmt.Default;
import org.yangcentral.yangkit.model.api.stmt.Deviate;
import org.yangcentral.yangkit.model.api.stmt.DeviateType;
import org.yangcentral.yangkit.model.api.stmt.Deviation;
import org.yangcentral.yangkit.model.api.stmt.Grouping;
import org.yangcentral.yangkit.model.api.stmt.IdentifierRef;
import org.yangcentral.yangkit.model.api.stmt.IfFeature;
import org.yangcentral.yangkit.model.api.stmt.Import;
import org.yangcentral.yangkit.model.api.stmt.Include;
import org.yangcentral.yangkit.model.api.stmt.Input;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.Must;
import org.yangcentral.yangkit.model.api.stmt.Output;
import org.yangcentral.yangkit.model.api.stmt.Prefix;
import org.yangcentral.yangkit.model.api.stmt.Referencable;
import org.yangcentral.yangkit.model.api.stmt.Refine;
import org.yangcentral.yangkit.model.api.stmt.SchemaNode;
import org.yangcentral.yangkit.model.api.stmt.SchemaNodeContainer;
import org.yangcentral.yangkit.model.api.stmt.Type;
import org.yangcentral.yangkit.model.api.stmt.Uses;
import org.yangcentral.yangkit.model.api.stmt.XPathSupport;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.model.api.stmt.YangUnknown;
import org.yangcentral.yangkit.model.impl.stmt.ImportImpl;
import org.yangcentral.yangkit.model.impl.stmt.IncludeImpl;
import org.yangcentral.yangkit.model.impl.stmt.PrefixImpl;
import org.yangcentral.yangkit.parser.YangParserException;
import org.yangcentral.yangkit.parser.YangYinParser;
import org.yangcentral.yangkit.utils.file.FileUtil;
import org.yangcentral.yangkit.writter.YangFormatter;
import org.yangcentral.yangkit.writter.YangWriter;

import org.dom4j.DocumentException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * ????????????
 *
 * @author f00360218
 * @since 2022-05-13
 */
public class YangTransformer {


    public static boolean hasInActiveDescendant(SchemaNodeContainer schemaNodeContainer){
        for(SchemaNode child:schemaNodeContainer.getSchemaNodeChildren()){
            if(!child.isActive()){
                return true;
            }
            if(child instanceof SchemaNodeContainer){
                if(hasInActiveDescendant((SchemaNodeContainer) child)){
                    return true;
                }
            }
        }
        return false;
    }

    private static Import getImport(Module curModule, String importModule){
        List<Import> imports =  curModule.getImports();
        for(Import im:imports){
            if(im.getArgStr().equals(importModule)){
                return im;
            }
        }
        return null;
    }

    private static Include getInclude(Module curModule, String includeModule){
        List<Include> includes =  curModule.getIncludes();
        for(Include include:includes){
            if(include.getArgStr().equals(includeModule)){
                return include;
            }
        }
        return null;
    }

    private static LinkageInfo genLinkageInfoForIdentifierRef(Module curModule,IdentifierRef statement){
        YangStatement referStmt = statement.getReferenceStatement();
        if(referStmt == null){
            return null;
        }
        Module originalModule = referStmt.getContext().getCurModule();
        if(originalModule == curModule){
            //the same module or submodule,no linkage should be added
            return null;
        }
        if(originalModule.getMainModule() == curModule.getMainModule()){
            //belongs to the same module but in several submodules, a new include should be added.
            Include include = getInclude(curModule,originalModule.getArgStr());
            if(null == include){
                LinkageInfo linkageInfo = new LinkageInfo(LinkageType.INCLUDE,originalModule.getModuleId());
                return linkageInfo;
            }
            return null;
        }
        else {
            //belongs to other modules, a new import should be added
            Import im = getImport(curModule,originalModule.getMainModule().getArgStr());
            LinkageInfo linkageInfo = null;
            if( im == null){
                linkageInfo = new LinkageInfo(LinkageType.IMPORT,originalModule.getMainModule().getModuleId());
                linkageInfo.setPrefix(originalModule.getMainModule().getSelfPrefix());
            }
            //process argument
            YangStatement yangStatement = (YangStatement) statement;
            String localArg = new FName(yangStatement.getArgStr()).getLocalName();
            yangStatement.setArgStr(
                ((im != null) ? im.getPrefix().getArgStr() : originalModule.getMainModule().getSelfPrefix()) + ":" + localArg);

            return linkageInfo;
        }
    }
    private static List<TransformerResult> transformXPathSupport(Module curModule,XPathSupport xPathSupport){
        List<TransformerResult> transformerResults = new ArrayList<>();
        YangStatement statement = (YangStatement) xPathSupport;
        YangXPathTransformerVisitor visitor = new YangXPathTransformerVisitor(xPathSupport,curModule);
        transformerResults.addAll(visitor.visit(((XPathSupport) statement).getXPathExpression().getRootExpr(), (XPathSupport) statement));
        return transformerResults;
    }

    private static YangStatement getParentNoUses(Uses target){
        if(target.getParentSchemaNode() instanceof Uses){
            return getParentNoUses((Uses) target.getParentSchemaNode());
        }
        return (YangStatement) target.getParentSchemaNode();
    }
    private static void transformRefine(Refine refine){
        SchemaNode target = refine.getTarget();
        if(refine.getDefaults().size() > 0){
            List<YangStatement> defaults = target.getSubStatement(YangBuiltinKeyword.DEFAULT.getQName());
            if(defaults.size() > 0){
                for(YangStatement dflt:defaults){
                    target.removeChild(dflt);
                }
            }
            for(Default dflt: refine.getDefaults()){
                target.addChild(dflt);
            }
        }
        if(refine.getDescription() != null){
            List<YangStatement> descriptions = target.getSubStatement(YangBuiltinKeyword.DESCRIPTION.getQName());
            if(descriptions.size() > 0){
                target.removeChild(descriptions.get(0));
            }
            target.addChild(refine.getDescription());
        }
        if(refine.getReference() != null){
            List<YangStatement> references = target.getSubStatement(YangBuiltinKeyword.REFERENCE.getQName());
            if(references.size() > 0){
                target.removeChild(references.get(0));
            }
            target.addChild(refine.getReference());
        }
        if(refine.getConfig() != null){
            List<YangStatement> configs = target.getSubStatement(YangBuiltinKeyword.CONFIG.getQName());
            if(configs.size() > 0){
                target.removeChild(configs.get(0));
            }
            target.addChild(refine.getConfig());
        }
        if(refine.getMandatory() != null){
            List<YangStatement> mandatorys = target.getSubStatement(YangBuiltinKeyword.MANDATORY.getQName());
            if(mandatorys.size() > 0){
                target.removeChild(mandatorys.get(0));
            }
            target.addChild(refine.getMandatory());
        }
        if(refine.getPresence() != null){
            List<YangStatement> presences = target.getSubStatement(YangBuiltinKeyword.PRESENCE.getQName());
            if(presences.size() > 0){
                target.removeChild(presences.get(0));
            }
            target.addChild(refine.getPresence());
        }
        if(refine.getMusts().size() > 0){
            for(Must must: refine.getMusts()){
                target.addChild(must);
            }
        }
        if(refine.getMinElements() != null){
            List<YangStatement> minElementses = target.getSubStatement(YangBuiltinKeyword.MINELEMENTS.getQName());
            if(minElementses.size() > 0){
                target.removeChild(minElementses.get(0));
            }
            target.addChild(refine.getMinElements());
        }
        if(refine.getMaxElements() != null){
            List<YangStatement> maxElementses = target.getSubStatement(YangBuiltinKeyword.MAXELEMENTS.getQName());
            if(maxElementses.size() > 0){
                target.removeChild(maxElementses.get(0));
            }
            target.addChild(refine.getMaxElements());
        }
        if(refine.getIfFeatures().size() > 0){
            for(IfFeature ifFeature: refine.getIfFeatures()){
                target.addChild(ifFeature);
            }
        }
        if(refine.getUnknowns().size() > 0){
            for(YangUnknown unknown: refine.getUnknowns()){
                target.addChild(unknown);
            }
        }
    }
    private static List<TransformerResult> transformExpand(SchemaNode schemaNode){
        List<TransformerResult> transformerResults = new ArrayList<>();
        if(schemaNode instanceof Uses){
            Uses uses = (Uses) schemaNode;
            YangStatement target = (YangStatement) schemaNode.getParentSchemaNode();
            if(target== null || !(target instanceof SchemaNode)){
                target = schemaNode.getParentStatement();
            } else {
                if (target instanceof Uses) {
                    target = getParentNoUses((Uses) target);
                }
                else if(target instanceof Case){
                    Case c = (Case) target;
                    if(c.isShortCase()){
                        target = c.getParent();
                    }
                }
            }
            if(uses.getRefines().size() > 0){
                for(Refine refine:uses.getRefines()){
                    transformRefine(refine);
                }
            }
            if(uses.getAugments().size() > 0){
                for(Augment augment:uses.getAugments()){
                    SchemaNode augmentTarget = augment.getTarget();
                    List<SchemaNode> schemaNodes = augment.getSchemaNodeChildren();
                    for(SchemaNode son:schemaNodes){
                        augmentTarget.addChild(son);
                    }
                }
            }
            //if (hasInActiveDescendant(uses)) {
                TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE, target, new ArrayList<>());
                transformerResult.addStatement(null, schemaNode);
                transformerResults.add(transformerResult);
                uses.getRefGrouping().delReference(uses);
                List<SchemaNode> schemaNodes = uses.getSchemaNodeChildren();
                TransformerResult sonTransformerResult = new TransformerResult(TransformerType.ADD, target, new ArrayList<>());
                YangStatement pre = uses;
                for (SchemaNode sonSchemaNode : schemaNodes) {
                    sonTransformerResult.addStatement(pre, sonSchemaNode);
                    pre = sonSchemaNode;
                }
                transformerResults.add(sonTransformerResult);
                //return transformerResults;
            //}
        }
        if(schemaNode instanceof SchemaNodeContainer){
            transformerResults.addAll(procSchemaNodeChildren((SchemaNodeContainer) schemaNode));
        }
        return transformerResults;
    }

    private static boolean isUnUsed(YangStatement statement){
        YangStatement ancestor = getAncestorStatement(statement);
        if( !(ancestor instanceof Module)){
            //it means this statement has been deleted from module
            return true;
        }
        YangContext context = statement.getContext();
        if(null == context){
            return true;
        }
        Grouping curGrouping = context.getCurGrouping();
        if(curGrouping != null){
            //if this statement in grouping, if the grouping is unused, so does this statement
            if(isUnUsed(curGrouping)){
                return true;
            }
        }
        if(statement instanceof Referencable){
            //if statement is referencable, need judge all refs
            Referencable referencable = (Referencable) statement;
            List<YangStatement> refs = referencable.getReferencedBy();
            for(YangStatement ref:refs){
                if(!isUnUsed(ref)){
                    return false;
                }
            }
            return true;
        } else if (statement instanceof SchemaNode){
            SchemaNode schemaNode = (SchemaNode) statement;
            if(!schemaNode.isActive()){
                return true;
            }
        } else if(statement instanceof Type){
            Type type = (Type) statement;
            YangStatement parent = type.getParentStatement();
            if(isUnUsed(parent)){
                return true;
            }
        }
        return false;
    }

    /**
     * delete deviation(not-supported),inactive node, delete uses who has inactive descendent and add active node.
     * @param curModule
     * @param statement
     * @return
     */
    private static List<TransformerResult> transformStmt(Module curModule,YangStatement statement) {
        List<TransformerResult> transformerResults = new ArrayList<>();
        if (statement instanceof Deviation) {
            Deviation deviation = (Deviation) statement;
            if (deviation.getDeviates().size() == 1) {
                Deviate deviate = deviation.getDeviates().get(0);
                if (deviate.getDeviateType() == DeviateType.NOT_SUPPORTED) {
                    TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE, deviation.getParentStatement(),
                        new ArrayList<>());
                    transformerResult.addStatement(null, statement);
                    transformerResults.add(transformerResult);
                    return transformerResults;
                }
            }
            for(Deviate deviate: deviation.getDeviates()){
                switch (deviate.getDeviateType()){
                    case ADD:{
                        // List<Must> musts = deviate.getMusts();
                        // if(musts.size() > 0){
                        //     TransformerResult transformerResult = new TransformerResult(TransformerType.ADD,deviation.getTarget(),new ArrayList<>());
                        //     TransformerResult transformerSelfResult = new TransformerResult(TransformerType.DELETE,deviate,new ArrayList<>());
                        //     for(Must must:musts){
                        //         transformerResult.addStatement(null,must);
                        //         transformerSelfResult.addStatement(null,must);
                        //     }
                        //     transformerResults.add(transformerResult);
                        //     transformerResults.add(transformerSelfResult);
                        //
                        // }
                        break;
                    }
                    case DELETE:{
                        List<Must> musts = deviate.getMusts();
                        if(musts.size() > 0){
                            List<YangStatement> targetMusts = deviation.getTarget().getSubStatement(YangBuiltinKeyword.MUST.getQName());
                            TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE,deviation.getTarget(),new ArrayList<>());
                            TransformerResult transformerSelfResult = new TransformerResult(TransformerType.DELETE,deviate,new ArrayList<>());
                            for(Must must:musts){
                                for(YangStatement targetStmt:targetMusts){
                                    if(targetStmt.equals(must)){
                                        transformerResult.addStatement(null,targetStmt);
                                        break;
                                    }
                                }
                                transformerSelfResult.addStatement(null,must);
                            }
                            transformerResults.add(transformerResult);
                            transformerResults.add(transformerSelfResult);
                        }
                        break;
                    }
                    case REPLACE:{
                        if(deviate.getType() != null){
                            Type targetType =null;
                            int pos = -1;
                            for(int i = 0; i < deviation.getTarget().getSubElements().size();i++){
                                YangElement subElement = deviation.getTarget().getSubElements().get(i);
                                if(!(subElement instanceof YangStatement)){
                                    continue;
                                }
                                YangStatement subStatement = (YangStatement) subElement;
                                if(subStatement.getYangKeyword().equals(YangBuiltinKeyword.TYPE.getQName())){
                                    targetType = (Type) subStatement;
                                    pos = i;
                                    break;
                                }
                            }
                            if(targetType.getRestriction() instanceof LeafRef){
                                deviate.getTarget().getSubElements().set(pos,deviate.getType());
                            }
                        }
                        break;
                    }
                }
            }
        } else if (statement instanceof SchemaNode) {
            SchemaNode schemaNode = (SchemaNode) statement;
            if(!schemaNode.isActive()){
                YangStatement target = schemaNode.getParentStatement();
                if(target instanceof Case){
                    Case c = (Case) target;
                    if(c.isShortCase()){
                        target = c.getParent();
                    }
                }
                TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE, target, new ArrayList<>());
                transformerResult.addStatement(null, statement);
                transformerResults.add(transformerResult);
                return transformerResults;
            }

        }

        transformerResults.addAll(procSubElement(curModule,statement,TransformerPhase.PHASE_STATEMENT));


        return transformerResults;
    }

    public static TransformerResult getTransformerResultFromLinkage(LinkageInfo linkageInfo,Module curModule){
        if(linkageInfo == null){
            return null;
        }
        YangStatement candidate = null;
        if(linkageInfo.getType() == LinkageType.IMPORT){
            YangContext newImportContext = new YangContext(curModule.getContext());
            Import newImport = new ImportImpl(linkageInfo.getModuleId().getModuleName());
            newImport.setContext(newImportContext);
            Prefix prefixStmt = new PrefixImpl(linkageInfo.getPrefix());
            YangContext prefixContext = new YangContext(newImportContext);
            prefixStmt.setContext(prefixContext);
            newImport.addChild(prefixStmt);
            candidate = newImport;
        }
        else {
            YangContext newIncludeContext = new YangContext(curModule.getContext());
            Include newInclude = new IncludeImpl(linkageInfo.getModuleId().getModuleName());
            newInclude.setContext(newIncludeContext);
            candidate = newInclude;
        }
        TransformerResult transformerResult = new TransformerResult(TransformerType.ADD,curModule,new ArrayList<>());
        transformerResult.addStatement(null,candidate);
        return transformerResult;
    }
    private static List getUniqueList(List original){
        List list = new ArrayList<>();
        for(Object item :original){
            if(list.contains(item)){
                continue;
            }
            list.add(item);
        }
        return list;
    }

    private static List<TransformerResult> transformLinkage(Module curModule,YangStatement statement){
        Module oriModule = statement.getContext().getCurModule();
        List<TransformerResult> transformerResults = new ArrayList<>();
        if(oriModule != curModule){
            if(statement instanceof IdentifierRef){
                LinkageInfo linkageInfo = genLinkageInfoForIdentifierRef(curModule, (IdentifierRef) statement);
                if(linkageInfo != null){
                    TransformerResult transformerResult = getTransformerResultFromLinkage(linkageInfo,curModule);
                    transformerResults.add(transformerResult);
                }
            } else if (statement instanceof XPathSupport){
                transformerResults.addAll(transformXPathSupport(curModule, (XPathSupport) statement));
                //TODO fix bug  reformat xpath expression
            } else if (statement instanceof IfFeature){
                //TODO
                IfFeature ifFeature = (IfFeature) statement;
                IfFeatureExprVisitor ifFeatureExprVisitor = new IfFeatureExprVisitor();
                List<String> prefixes = ifFeatureExprVisitor.visit(ifFeature.getIfFeatureExpr());
                List list = getUniqueList(prefixes);
                for(Object o:list){
                    String prefix = (String) o;
                    ModuleId moduleId = ifFeature.getContext().getCurModule().getPrefixes().get(prefix);
                    if(!curModule.getModuleId().equals(moduleId) && curModule.getImportByPrefix(prefix) == null){
                        LinkageInfo linkageInfo = new LinkageInfo(LinkageType.IMPORT,moduleId);
                        linkageInfo.setPrefix(prefix);
                        transformerResults.add(getTransformerResultFromLinkage(linkageInfo,curModule));
                    }
                }
            }

        }
        transformerResults.addAll(procSubElement(curModule,statement,TransformerPhase.PHASE_LINKAGE));
        return transformerResults;
    }

    private static YangStatement getAncestorStatement(YangStatement statement){
        YangStatement ancestor = statement;
        YangStatement parent = statement.getParentStatement();
        while (parent != null){
            if(parent instanceof Case){
                Case c = (Case) parent;
                if(c.isShortCase()){
                    parent = c.getParent();
                }
            }
            ancestor = parent;
            parent = ancestor.getParentStatement();
        }
        return ancestor;
    }
    private static boolean isEmptyStatement(YangStatement statement){
        for(YangElement subElement:statement.getSubElements()){
            if(subElement instanceof YangStatement){
                return false;
            }
        }
        return true;
    }

    private static List<TransformerResult> transformUnused(Module curModule,YangStatement statement){
        List<TransformerResult> transformerResults = new ArrayList<>();
        if(statement instanceof Referencable){
            if(isUnUsed(statement)){
                TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE, statement.getParentStatement(), new ArrayList<>());
                transformerResult.addStatement(null, statement);
                transformerResults.add(transformerResult);
                return transformerResults;
            }
        } else if(statement instanceof Augment || statement instanceof Input || statement instanceof Output){
            if(statement.getSubElements().size() == 0){
                TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE,statement.getParentStatement(),new ArrayList<>());
                transformerResult.addStatement(null,statement);
                transformerResults.add(transformerResult);
                return transformerResults;
            }
        } else if(statement instanceof Deviation){
            Deviation deviation = (Deviation) statement;
            List<Deviate> deviates = deviation.getDeviates();
            boolean isActive = false;
            for(Deviate deviate:deviates){
                if(isEmptyStatement(deviate)){
                    TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE,statement,new ArrayList<>());
                    transformerResult.addStatement(null,deviate);
                    transformerResults.add(transformerResult);
                    continue;
                }
                isActive = true;
            }
            if(!isActive){
                TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE,statement.getParentStatement(),new ArrayList<>());
                transformerResult.addStatement(null,statement);
                transformerResults.add(transformerResult);
            }
        }
        transformerResults.addAll(procSubElement(curModule,statement,TransformerPhase.PHASE_UNUSED));
        return transformerResults;
    }

    private static List<String> lookupPrefixes(Module curModule,YangStatement statement){
        List<String> transformerResults = new ArrayList<>();
        if(statement instanceof IdentifierRef){
            FName fName = new FName(statement.getArgStr());
            if(fName.getPrefix() != null){
                transformerResults.add(fName.getPrefix());
            }
        }  else if(statement instanceof IfFeature){
            IfFeature ifFeature = (IfFeature) statement;
            IfFeature.IfFeatureExpr ifFeatureExpr = ifFeature.getIfFeatureExpr();
            transformerResults.addAll(new IfFeatureExprVisitor().visit(ifFeatureExpr));
        } else if(statement instanceof XPathSupport){
            XPathSupport xPathSupport = (XPathSupport) statement;
            YangXPathPrefixVisitor pathPrefixVisitor = new YangXPathPrefixVisitor((XPathSupport) statement,curModule);
            transformerResults.addAll(pathPrefixVisitor.visit(xPathSupport.getXPathExpression().getRootExpr(),xPathSupport));
        } else if (statement instanceof YangUnknown){
            YangUnknown yangUnknown = (YangUnknown) statement;
            FName fName = new FName(yangUnknown.getKeyword());
            if(fName.getPrefix() != null){
                transformerResults.add(fName.getPrefix());
            }
        } else if (statement instanceof DataNodeModifier){
            DataNodeModifier dataNodeModifier = (DataNodeModifier) statement;
            List<QName> steps = dataNodeModifier.getTargetPath().getPath();
            for(QName step:steps){
                if(step.getPrefix() != null){
                    transformerResults.add(step.getPrefix());
                }
            }
        } else if (statement instanceof Default){
            FName fName = new FName(statement.getArgStr());
            if(fName.getPrefix() != null){
                transformerResults.add(fName.getPrefix());
            }
        }
        if(statement.getSubElements().size() > 0){
            List<String> sonTransformerResults = new ArrayList<>();
            for(YangElement subElement: statement.getSubElements()){
                if(subElement instanceof YangStatement){
                    List<String> sonTransformerResult = lookupPrefixes(curModule,(YangStatement) subElement);
                    sonTransformerResults.addAll(sonTransformerResult);
                }
            }
            transformerResults.addAll(sonTransformerResults);
        }
        return transformerResults;
    }
    private static List<TransformerResult> procSchemaNodeChildren(SchemaNodeContainer statement){
        List<TransformerResult> transformerResults = new ArrayList<>();
        if(statement.getSchemaNodeChildren().size() > 0){
            for(SchemaNode schemaNode: statement.getSchemaNodeChildren()){
                List<TransformerResult> sonTransformerResult = transformExpand(schemaNode);
                transformerResults.addAll(sonTransformerResult);
            }
        }
        return transformerResults;
    }

    private static List<TransformerResult> procSubElement(Module curModule, YangStatement statement,TransformerPhase phase){
        List<TransformerResult> transformerResults = new ArrayList<>();
        if(statement.getSubElements().size() > 0){
            List<TransformerResult> sonTransformerResults = new ArrayList<>();
            for(YangElement subElement: statement.getSubElements()){
                if(subElement instanceof YangStatement){
                    List<TransformerResult> sonTransformerResult = transform(curModule,(YangStatement) subElement,phase);
                    sonTransformerResults.addAll(sonTransformerResult);
                }
            }
            transformerResults.addAll(sonTransformerResults);
        }
        return transformerResults;
    }
    public static List<TransformerResult> procUnusedLinkage(Module curModule){
        List<TransformerResult> transformerResults = new ArrayList<>();
        List<String> prefixes = lookupPrefixes(curModule,curModule);
        List<Import> imports = curModule.getImports();
        for(Import im:imports){
            if(!prefixes.contains(im.getPrefix().getArgStr())){
                TransformerResult transformerResult = new TransformerResult(TransformerType.DELETE,curModule,new ArrayList<>());
                transformerResult.addStatement(null,im);
                transformerResults.add(transformerResult);
            }
        }
        return transformerResults;
    }
    public static List<TransformerResult> transform(Module curModule,YangStatement statement,TransformerPhase phase){
        switch (phase){
            case PHASE_STATEMENT:{
                return transformStmt(curModule,statement);
            }
            case PHASE_LINKAGE:{
                return transformLinkage(curModule,statement);
            }
            case PHASE_UNUSED:{
                return transformUnused(curModule,statement);
            }
        }
        return null;
    }

    private static void insert(YangStatement target, YangStatement pre,YangStatement candidate){
        //judge whether the candidate is already sub element of target
        for (int j = 0; j < target.getSubElements().size(); j++) {
            YangElement subElement = target.getSubElements().get(j);
            if (subElement instanceof YangStatement) {
                YangStatement subStatement = (YangStatement) subElement;
                if (subStatement.equals(candidate)) {
                    return;
                }
            }
        }
        //lookup the insert position

        int pos = -1;
        for (int j = 0; j < target.getSubElements().size(); j++) {
            YangElement subElement = target.getSubElements().get(j);
            if (subElement instanceof YangStatement) {
                YangStatement subStatement = (YangStatement) subElement;
                if(pre != null){
                    if(subStatement == pre){
                        pos =  j+1;
                        break;
                    }
                }
                else {
                    if (subStatement.getYangKeyword().equals(candidate.getYangKeyword())) {
                        pos = j + 1;
                    }
                }
            }
        }
        if(pos != -1){
            target.addChild(pos,candidate);
        }
        else {
            target.addChild(candidate);
        }

    }

    private static void procTransformerResult(List<TransformerResult> transformerResults){
        //add
        for(TransformerResult transformerResult:transformerResults) {
            YangStatement target = transformerResult.getTarget();
            if (target == null) {
                assert false;
            }
            if (transformerResult.getType() == TransformerType.ADD) {
                for (TransformerStatement candidate : transformerResult.getStatements()) {
                    insert(target,candidate.getPre(), candidate.getStatement());
                }
            }
        }
        //delete
        for(TransformerResult transformerResult:transformerResults) {
            YangStatement target = transformerResult.getTarget();
            if (target == null) {
                assert false;
            }
            if (transformerResult.getType() == TransformerType.DELETE) {
                for(TransformerStatement candidate:transformerResult.getStatements()){
                    target.removeChild(candidate.getStatement());
                }
            }
        }
    }

    private static void transformUnUsed(YangSchemaContext schemaContext){
        Iterator<Map.Entry<String, List<YangElement>>> it = schemaContext.getParseResult().entrySet().iterator();
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
                    //phase_unused
                    List<TransformerResult> transformerResults = transform(curModule,curModule,TransformerPhase.PHASE_UNUSED);
                    procTransformerResult(transformerResults);
                }

            }
        }
    }
    private static void transformUnUsedLinkage(YangSchemaContext schemaContext){
        Iterator<Map.Entry<String, List<YangElement>>> it = schemaContext.getParseResult().entrySet().iterator();
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
                    //delete unused linkage
                    List<TransformerResult> transformerResults = procUnusedLinkage(curModule);
                    procTransformerResult(transformerResults);
                }

            }
        }
    }
    private static void transformStmt(YangSchemaContext schemaContext){
        Iterator<Map.Entry<String, List<YangElement>>> it = schemaContext.getParseResult().entrySet().iterator();
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
                    //phase_stmt
                    List<TransformerResult> transformerResults = transform(curModule,curModule,TransformerPhase.PHASE_STATEMENT);
                    procTransformerResult(transformerResults);
                }

            }
        }
    }
    private static void transformLinkage(YangSchemaContext schemaContext){
        Iterator<Map.Entry<String, List<YangElement>>> it = schemaContext.getParseResult().entrySet().iterator();
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
                    //phase_stmt
                    List<TransformerResult> transformerResults  = transform(curModule,curModule,TransformerPhase.PHASE_LINKAGE);
                    procTransformerResult(transformerResults);
                }

            }
        }
    }
    public static void transform(YangSchemaContext schemaContext){
        List<SchemaNode> schemaNodes = schemaContext.getSchemaNodeChildren();
        for(SchemaNode schemaNode:schemaNodes){
            List<TransformerResult> transformerResults = transformExpand(schemaNode);
            procTransformerResult(transformerResults);
        }
        transformStmt(schemaContext);
        transformLinkage(schemaContext);
        transformUnUsed(schemaContext);
        transformUnUsedLinkage(schemaContext);

    }

    public static void main(String[] args){
        String yangDir = args[0];
        String outDir = args[1];
        File outDirFile = new File(outDir);
        if(!outDirFile.exists()){
            outDirFile.mkdirs();
        }
        try {
            YangSchemaContext schemaContext = YangYinParser.parse(yangDir);
            ValidatorResult validatorResult = schemaContext.validate();
            //System.out.println(validatorResult);
            transform(schemaContext);
            Iterator<Map.Entry<String, List<YangElement>>> it = schemaContext.getParseResult().entrySet().iterator();
            while(it.hasNext()){
                Map.Entry<String, List<YangElement>> entry = it.next();
                List<YangElement> elements = entry.getValue();
                StringBuffer sb= new StringBuffer();
                for(YangElement element:elements){
                    String str = YangWriter.toYangString(element,YangFormatter.getPrettyYangFormatter()," ");
                    sb.append(str);
                    sb.append("\n");
                }
                String fileName= entry.getKey().substring(entry.getKey().lastIndexOf("\\"));

                File outFile = new File(outDir,fileName);
                if(!outFile.exists()){
                    outFile.createNewFile();
                }
                FileUtil.writeUtf8File(sb.toString(),outFile);

            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (YangParserException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }

    }
}
