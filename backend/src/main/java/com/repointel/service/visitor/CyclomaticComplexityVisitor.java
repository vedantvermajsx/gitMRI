package com.repointel.service.visitor;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * Computes McCabe's Cyclomatic Complexity for a single method.
 *
 * CC = 1 + number of branching points:
 *   if, else-if, for, foreach, while, do-while, case, catch, ternary, &&, ||
 */
public class CyclomaticComplexityVisitor extends VoidVisitorAdapter<Void> {

    private int complexity = 1; // base complexity

    public int getComplexity() {
        return complexity;
    }

    @Override
    public void visit(IfStmt n, Void arg) {
        complexity++;
        super.visit(n, arg);
    }

    @Override
    public void visit(ForStmt n, Void arg) {
        complexity++;
        super.visit(n, arg);
    }

    @Override
    public void visit(ForEachStmt n, Void arg) {
        complexity++;
        super.visit(n, arg);
    }

    @Override
    public void visit(WhileStmt n, Void arg) {
        complexity++;
        super.visit(n, arg);
    }

    @Override
    public void visit(DoStmt n, Void arg) {
        complexity++;
        super.visit(n, arg);
    }

    @Override
    public void visit(SwitchEntry n, Void arg) {
        // Each case label (except default) adds a branch
        if (!n.getLabels().isEmpty()) {
            complexity++;
        }
        super.visit(n, arg);
    }

    @Override
    public void visit(CatchClause n, Void arg) {
        complexity++;
        super.visit(n, arg);
    }

    @Override
    public void visit(ConditionalExpr n, Void arg) {
        complexity++; // ternary operator
        super.visit(n, arg);
    }

    @Override
    public void visit(BinaryExpr n, Void arg) {
        // && and || add branches
        if (n.getOperator() == BinaryExpr.Operator.AND ||
            n.getOperator() == BinaryExpr.Operator.OR) {
            complexity++;
        }
        super.visit(n, arg);
    }
}
