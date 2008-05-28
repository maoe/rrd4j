package org.rrd4j.data;

class CDef extends Source {
    private String rpnExpression;

    CDef(String name, String rpnExpression) {
        super(name);
        this.rpnExpression = rpnExpression;
    }

    String getRpnExpression() {
        return rpnExpression;
    }
}
