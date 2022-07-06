package com.huawei.yang.transformer;

import org.yangcentral.yangkit.model.api.stmt.IfFeature;
import org.yangcentral.yangkit.model.impl.stmt.IfFeatureImpl;

import java.util.ArrayList;
import java.util.List;

/**
 * 功能描述
 *
 * @author f00360218
 * @since 2022-05-26
 */
public class IfFeatureExprVisitor {

    public List<String> visitExpression(IfFeatureImpl.Expression expression){
        List<String> prefixes = new ArrayList<>();
        IfFeatureImpl.Term term = expression.getIfFeatureTerm();
        if(term != null){
            prefixes.addAll(visit(term));
        }
        IfFeatureImpl.Expression another = expression.getAnother();
        if(another != null){
            prefixes.addAll(visit(another));
        }
        return prefixes;
    }

    public List<String> visitFactor(IfFeatureImpl.Factor factor){
        List<String> prefixes = new ArrayList<>();
        if(factor instanceof IfFeatureImpl.NotFactor){
            IfFeatureImpl.NotFactor notFactor = (IfFeatureImpl.NotFactor) factor;
            prefixes.addAll(visit(notFactor.getFactor()));
        } else if (factor instanceof IfFeatureImpl.GroupFactor){
            IfFeatureImpl.GroupFactor groupFactor = (IfFeatureImpl.GroupFactor) factor;
            prefixes.addAll(visit(groupFactor.getExpression()));
        } else if(factor instanceof IfFeatureImpl.RefFactor){
            IfFeatureImpl.RefFactor refFactor = (IfFeatureImpl.RefFactor) factor;
            prefixes.add(refFactor.getRefFeature().getPrefix());
        }
        return prefixes;
    }

    public List<String> visitTerm(IfFeatureImpl.Term term){
        List<String> prefixes = new ArrayList<>();
        IfFeatureImpl.Term another = term.getAnother();
        if(another != null){
            prefixes.addAll(visit(another));
        }
        IfFeatureImpl.Factor factor = term.getFactor();
        if(factor != null){
            prefixes.addAll(visit(factor));
        }
        return prefixes;
    }

    public List<String> visit(IfFeature.IfFeatureExpr ifFeatureExpr){
        if(ifFeatureExpr instanceof IfFeatureImpl.Expression){
            return visitExpression((IfFeatureImpl.Expression) ifFeatureExpr);
        } else if(ifFeatureExpr instanceof IfFeatureImpl.Factor){
            return visitFactor((IfFeatureImpl.Factor) ifFeatureExpr);
        } else if (ifFeatureExpr instanceof IfFeatureImpl.Term){
            return visitTerm((IfFeatureImpl.Term) ifFeatureExpr);
        }
        return new ArrayList<>();
    }
}
