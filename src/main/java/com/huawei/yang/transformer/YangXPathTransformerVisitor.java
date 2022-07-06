package com.huawei.yang.transformer;

import org.yangcentral.yangkit.base.YangContext;
import org.yangcentral.yangkit.model.api.schema.ModuleId;
import org.yangcentral.yangkit.model.api.stmt.Import;
import org.yangcentral.yangkit.model.api.stmt.Include;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.Prefix;
import org.yangcentral.yangkit.model.api.stmt.XPathSupport;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.model.impl.stmt.ImportImpl;
import org.yangcentral.yangkit.model.impl.stmt.IncludeImpl;
import org.yangcentral.yangkit.model.impl.stmt.PrefixImpl;
import org.yangcentral.yangkit.xpath.YangXPathVisitor;

import org.jaxen.expr.AdditiveExpr;
import org.jaxen.expr.BinaryExpr;
import org.jaxen.expr.EqualityExpr;
import org.jaxen.expr.Expr;
import org.jaxen.expr.FilterExpr;
import org.jaxen.expr.FunctionCallExpr;
import org.jaxen.expr.LiteralExpr;
import org.jaxen.expr.LocationPath;
import org.jaxen.expr.LogicalExpr;
import org.jaxen.expr.MultiplicativeExpr;
import org.jaxen.expr.NameStep;
import org.jaxen.expr.NumberExpr;
import org.jaxen.expr.PathExpr;
import org.jaxen.expr.Predicate;
import org.jaxen.expr.RelationalExpr;
import org.jaxen.expr.Step;
import org.jaxen.expr.UnaryExpr;
import org.jaxen.expr.UnionExpr;
import org.jaxen.expr.VariableReferenceExpr;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 功能描述
 *
 * @author f00360218
 * @since 2022-05-17
 */
public class YangXPathTransformerVisitor implements YangXPathVisitor<List<TransformerResult>, XPathSupport> {
    private XPathSupport statement;
    private Module curModule;

    public YangXPathTransformerVisitor(XPathSupport statement, Module curModule) {
        this.statement = statement;
        this.curModule = curModule;
    }

    @Override
    public List<TransformerResult> visitAdditiveExpr(AdditiveExpr additiveExpr, XPathSupport statement) {
        return null;
    }

    @Override
    public List<TransformerResult> visitBinaryExpr(BinaryExpr binaryExpr, XPathSupport statement) {
        List<TransformerResult> transformerResults = new ArrayList<>();
        List<TransformerResult> left = visit(binaryExpr.getLHS(),statement);
        transformerResults.addAll(left);
        List<TransformerResult> right = visit(binaryExpr.getRHS(),statement);
        transformerResults.addAll(right);
        return transformerResults;
    }

    @Override
    public List<TransformerResult> visitEqualityExpr(EqualityExpr equalityExpr, XPathSupport statement) {
        return null;
    }

    @Override
    public List<TransformerResult> visitFilterExpr(FilterExpr filterExpr, XPathSupport statement) {
        List<TransformerResult> transformerResults = new ArrayList<>();
        Expr expr = filterExpr.getExpr();
        transformerResults.addAll(visit(expr,statement));
        List predicates = filterExpr.getPredicates();
        if(predicates != null && predicates.size()>0){
            for(Object o:predicates){
                Predicate predicate = (Predicate) o;
                Expr predicateExpr = predicate.getExpr();
                transformerResults.addAll(visit(predicateExpr,statement));
            }
        }
        return transformerResults;
    }

    private LinkageInfo getLinkageInfo(String prefix,YangStatement statement){
        ModuleId moduleId = curModule.getPrefixes().get(prefix);
        if(moduleId != null){
            return null;
        }
        moduleId = statement.getContext().getCurModule().getPrefixes().get(prefix);
        if(moduleId == null){
            return null;
        }
        Optional<Module> oriModuleop = statement.getContext().getSchemaContext().getModule(moduleId);
        if(!oriModuleop.isPresent()){
            return null;
        }
        if(curModule == oriModuleop.get()){
            return null;
        }
        if(oriModuleop.get().getMainModule() == curModule.getMainModule()){
            LinkageInfo linkageInfo = new LinkageInfo(LinkageType.INCLUDE, oriModuleop.get().getModuleId());
            return linkageInfo;
        }
        LinkageInfo linkageInfo = new LinkageInfo(LinkageType.IMPORT, oriModuleop.get().getMainModule().getModuleId());
        linkageInfo.setPrefix(prefix);
        return linkageInfo;
    }

    @Override
    public List<TransformerResult> visitFunctionCallExpr(FunctionCallExpr functionCallExpr, XPathSupport statement) {
        List<TransformerResult> transformerResults = new ArrayList<>();
        String prefix = functionCallExpr.getPrefix();
        if(prefix != null && prefix.length() > 0){
            LinkageInfo linkageInfo = getLinkageInfo(prefix, (YangStatement) statement);
            if(null != linkageInfo){
                transformerResults.add(YangTransformer.getTransformerResultFromLinkage(linkageInfo,curModule));
            }
        }

        List paras = functionCallExpr.getParameters();
        if(paras != null && paras.size() >0){
            for(Object o :paras){
                Expr para = (Expr) o;
                transformerResults.addAll(visit(para,statement));
            }
        }
        return transformerResults;
    }

    @Override
    public List<TransformerResult> visitLiteralExpr(LiteralExpr literalExpr, XPathSupport statement) {
        return new ArrayList<>();
    }

    @Override
    public List<TransformerResult> visitLocationExpr(LocationPath locationPath, XPathSupport statement) {
        List<TransformerResult> transformerResults = new ArrayList<>();
        List steps = locationPath.getSteps();
        if(steps != null && steps.size() > 0){
            for(Object o : steps){
                Step step = (Step) o;
                if(step instanceof NameStep){
                    NameStep nameStep = (NameStep) step;
                    String prefix = nameStep.getPrefix();
                    if(prefix != null && prefix.length() >0){
                        LinkageInfo linkageInfo = getLinkageInfo(prefix, (YangStatement) statement);
                        if(null != linkageInfo){
                            transformerResults.add(YangTransformer.getTransformerResultFromLinkage(linkageInfo,curModule));
                        }
                    }
                }
                List predicates = step.getPredicates();
                if(predicates != null && predicates.size()>0){
                    for(Object p:predicates){
                        Predicate predicate = (Predicate) p;
                        Expr predicateExpr = predicate.getExpr();
                        transformerResults.addAll(visit(predicateExpr,statement));
                    }
                }

            }
        }
        return transformerResults;
    }

    @Override
    public List<TransformerResult> visitLogicalExpr(LogicalExpr logicalExpr, XPathSupport statement) {
        return null;
    }

    @Override
    public List<TransformerResult> visitMultiplicativeExpr(MultiplicativeExpr multiplicativeExpr,
        XPathSupport statement) {
        return null;
    }

    @Override
    public List<TransformerResult> visitNumberExpr(NumberExpr numberExpr, XPathSupport statement) {
        return new ArrayList<>();
    }

    @Override
    public List<TransformerResult> visitPathExpr(PathExpr pathExpr, XPathSupport statement) {
        List<TransformerResult> transformerResults = new ArrayList<>();
        transformerResults.addAll(visitLocationExpr(pathExpr.getLocationPath(),statement));
        transformerResults.addAll(visit(pathExpr.getFilterExpr(),statement));
        return transformerResults;
    }

    @Override
    public List<TransformerResult> visitRelationalExpr(RelationalExpr relationalExpr, XPathSupport statement) {
        return null;
    }

    @Override
    public List<TransformerResult> visitUnaryExpr(UnaryExpr unaryExpr, XPathSupport statement) {
        return visit(unaryExpr.getExpr(),statement);
    }

    @Override
    public List<TransformerResult> visitUnionExpr(UnionExpr unionExpr, XPathSupport statement) {
        return null;
    }

    @Override
    public List<TransformerResult> visitVariableReferenceExpr(VariableReferenceExpr variableReferenceExpr,
        XPathSupport statement) {
        List<TransformerResult> transformerResults = new ArrayList<>();
        String prefix = variableReferenceExpr.getPrefix();
        if(prefix != null && prefix.length() > 0){
            LinkageInfo linkageInfo = getLinkageInfo(prefix, (YangStatement) statement);
            if(null != linkageInfo){
                transformerResults.add(YangTransformer.getTransformerResultFromLinkage(linkageInfo,curModule));
            }
        }

        return transformerResults;
    }

    @Override
    public List<TransformerResult> visit(Expr expr, XPathSupport statement) {
        if(expr instanceof BinaryExpr){
            return visitBinaryExpr((BinaryExpr) expr,statement);
        } else if (expr instanceof FilterExpr){
            return visitFilterExpr((FilterExpr) expr,statement);
        } else if (expr instanceof FunctionCallExpr){
            return visitFunctionCallExpr((FunctionCallExpr) expr,statement);
        } else if (expr instanceof LiteralExpr){
            return visitLiteralExpr((LiteralExpr) expr,statement);
        } else if (expr instanceof LocationPath){
            return visitLocationExpr((LocationPath) expr,statement);
        } else if (expr instanceof NumberExpr){
            return visitNumberExpr((NumberExpr) expr,statement);
        } else if (expr instanceof PathExpr){
            return visitPathExpr((PathExpr) expr,statement);
        } else if (expr instanceof UnaryExpr){
            return visitUnaryExpr((UnaryExpr) expr,statement);
        } else if (expr instanceof VariableReferenceExpr){
            return visitVariableReferenceExpr((VariableReferenceExpr) expr,statement);
        }
        throw new IllegalArgumentException("unrecognized expr type.");
    }
}
