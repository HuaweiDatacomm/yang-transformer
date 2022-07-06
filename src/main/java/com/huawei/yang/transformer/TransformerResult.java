package com.huawei.yang.transformer;

import org.yangcentral.yangkit.model.api.stmt.Uses;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 功能描述
 *
 * @author f00360218
 * @since 2022-05-16
 */
class TransformerResult {
    private TransformerType type;
    private YangStatement target;
    private List<TransformerStatement> statements = new ArrayList<>();

    public TransformerResult(TransformerType type, YangStatement target,List<TransformerStatement> statements) {
        this.type = type;
        this.target = target;
        this.statements = statements;
    }



    public TransformerType getType() {
        return type;
    }

    public YangStatement getTarget() {
        return target;
    }

    public List<TransformerStatement> getStatements() {
        return statements;
    }
    public void addStatement(YangStatement pre,YangStatement statement){
        TransformerStatement transformerStatement = new TransformerStatement(pre,statement);
        statements.add(transformerStatement);
    }
}
