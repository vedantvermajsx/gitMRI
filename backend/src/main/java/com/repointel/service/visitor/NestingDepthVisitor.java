package com.repointel.service.visitor;

import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * Tracks maximum nesting depth within a method body.
 * Nesting increases for: if, for, foreach, while, do, try, switch, synchronized.
 */
public class NestingDepthVisitor extends VoidVisitorAdapter<Void> {

    private int currentDepth = 0;
    private int maxDepth = 0;

    public int getMaxDepth() {
        return maxDepth;
    }

    private void enter() {
        currentDepth++;
        maxDepth = Math.max(maxDepth, currentDepth);
    }

    private void exit() {
        currentDepth--;
    }

    @Override
    public void visit(IfStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }

    @Override
    public void visit(ForStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }

    @Override
    public void visit(ForEachStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }

    @Override
    public void visit(WhileStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }

    @Override
    public void visit(DoStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }

    @Override
    public void visit(TryStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }

    @Override
    public void visit(SwitchStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }

    @Override
    public void visit(SynchronizedStmt n, Void arg) {
        enter();
        super.visit(n, arg);
        exit();
    }
}
