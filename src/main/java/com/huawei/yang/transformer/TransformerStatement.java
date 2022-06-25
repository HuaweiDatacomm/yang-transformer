package com.huawei.yang.transformer;

import com.huawei.yang.model.api.stmt.YangStatement;

/**
 * 功能描述
 *
 * @author f00360218
 * @since 2022-05-17
 */
class TransformerStatement {
    private YangStatement pre;
    private YangStatement statement;

    public TransformerStatement(YangStatement pre, YangStatement statement) {
        this.pre = pre;
        this.statement = statement;
    }

    public YangStatement getPre() {
        return pre;
    }

    public YangStatement getStatement() {
        return statement;
    }
}
