package com.huawei.yang.transformer;

import com.huawei.yang.base.YangContext;
import com.huawei.yang.common.api.FName;
import com.huawei.yang.model.api.stmt.Import;
import com.huawei.yang.model.api.stmt.Include;
import com.huawei.yang.model.api.stmt.Module;
import com.huawei.yang.model.api.stmt.Prefix;
import com.huawei.yang.model.api.stmt.XPathSupport;
import com.huawei.yang.model.api.stmt.YangStatement;
import com.huawei.yang.model.impl.stmt.ImportImpl;
import com.huawei.yang.model.impl.stmt.IncludeImpl;
import com.huawei.yang.model.impl.stmt.PrefixImpl;
import com.huawei.yang.xpath.YangXPathVisitor;

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

/**
 * 功能描述
 *
 * @author f00360218
 * @since 2022-05-26
 */
public class YangXPathPrefixVisitor implements YangXPathVisitor<List<String>, XPathSupport> {
    private XPathSupport statement;
    private Module curModule;

    public YangXPathPrefixVisitor(XPathSupport statement, Module curModule) {
        this.statement = statement;
        this.curModule = curModule;
    }

    @Override
    public List<String> visitAdditiveExpr(AdditiveExpr additiveExpr, XPathSupport xPathSupport) {
        return null;
    }

    @Override
    public List<String> visitBinaryExpr(BinaryExpr binaryExpr, XPathSupport xPathSupport) {
        List<String> transformerResults = new ArrayList<>();
        List<String> left = visit(binaryExpr.getLHS(),statement);
        transformerResults.addAll(left);
        List<String> right = visit(binaryExpr.getRHS(),statement);
        transformerResults.addAll(right);
        return transformerResults;
    }

    @Override
    public List<String> visitEqualityExpr(EqualityExpr equalityExpr, XPathSupport xPathSupport) {
        return null;
    }

    @Override
    public List<String> visitFilterExpr(FilterExpr filterExpr, XPathSupport xPathSupport) {
        List<String> transformerResults = new ArrayList<>();
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

    @Override
    public List<String> visitFunctionCallExpr(FunctionCallExpr functionCallExpr, XPathSupport xPathSupport) {
        List<String> transformerResults = new ArrayList<>();
        String prefix = functionCallExpr.getPrefix();
        if(prefix != null && prefix.length() > 0){
            transformerResults.add(prefix);
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
    public List<String> visitLiteralExpr(LiteralExpr literalExpr, XPathSupport xPathSupport) {
        List<String> transformerResults = new ArrayList<>();
        FName fName = new FName(literalExpr.getLiteral());
        if(fName.getPrefix() != null){
            transformerResults.add(fName.getPrefix());
        }
        return transformerResults;
    }

    @Override
    public List<String> visitLocationExpr(LocationPath locationPath, XPathSupport xPathSupport) {
        List<String> transformerResults = new ArrayList<>();
        List steps = locationPath.getSteps();
        if(steps != null && steps.size() > 0){
            for(Object o : steps){
                Step step = (Step) o;
                if(step instanceof NameStep){
                    NameStep nameStep = (NameStep) step;
                    String prefix = nameStep.getPrefix();
                    if(prefix != null && prefix.length() >0){
                        transformerResults.add(prefix);
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
    public List<String> visitLogicalExpr(LogicalExpr logicalExpr, XPathSupport xPathSupport) {
        return null;
    }

    @Override
    public List<String> visitMultiplicativeExpr(MultiplicativeExpr multiplicativeExpr, XPathSupport xPathSupport) {
        return null;
    }

    @Override
    public List<String> visitNumberExpr(NumberExpr numberExpr, XPathSupport xPathSupport) {
        return new ArrayList<>();
    }

    @Override
    public List<String> visitPathExpr(PathExpr pathExpr, XPathSupport xPathSupport) {
        List<String> transformerResults = new ArrayList<>();
        transformerResults.addAll(visitLocationExpr(pathExpr.getLocationPath(),statement));
        transformerResults.addAll(visit(pathExpr.getFilterExpr(),statement));
        return transformerResults;
    }

    @Override
    public List<String> visitRelationalExpr(RelationalExpr relationalExpr, XPathSupport xPathSupport) {
        return null;
    }

    @Override
    public List<String> visitUnaryExpr(UnaryExpr unaryExpr, XPathSupport xPathSupport) {
        return visit(unaryExpr.getExpr(),statement);
    }

    @Override
    public List<String> visitUnionExpr(UnionExpr unionExpr, XPathSupport xPathSupport) {
        return null;
    }

    @Override
    public List<String> visitVariableReferenceExpr(VariableReferenceExpr variableReferenceExpr,
        XPathSupport xPathSupport) {
        List<String> transformerResults = new ArrayList<>();
        String prefix = variableReferenceExpr.getPrefix();
        if(prefix != null && prefix.length() > 0){
            transformerResults.add(prefix);
        }

        return transformerResults;
    }

    @Override
    public List<String> visit(Expr expr, XPathSupport xPathSupport) {
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
