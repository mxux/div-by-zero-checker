package org.checkerframework.checker.dividebyzero;

import com.sun.source.tree.*;
import org.checkerframework.checker.dividebyzero.qual.NonZero;
import org.checkerframework.checker.dividebyzero.qual.Top;
import org.checkerframework.checker.dividebyzero.qual.Zero;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.treeannotator.ListTreeAnnotator;
import org.checkerframework.framework.type.treeannotator.TreeAnnotator;
import org.checkerframework.javacutil.AnnotationBuilder;

import javax.lang.model.element.AnnotationMirror;
import java.lang.annotation.Annotation;

public class DivByZeroAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Compute the default annotation for the given literal.
   *
   * @param literal the literal in the syntax tree to examine
   * @return the most specific possible point in the lattice for the given literal
   */
  private Class<? extends Annotation> defaultAnnotation(LiteralTree literal) {
    switch (literal.getKind()) {
      case INT_LITERAL:
        int intValue = (Integer) literal.getValue();
        if (intValue == 0) {
          return Zero.class;
        } else {
          return NonZero.class;
        }
      case LONG_LITERAL:
        long longValue = (Long) literal.getValue();
        if (longValue == 0) {
          return Zero.class;
        } else {
          return NonZero.class;
        }
      default:
        return Top.class;
    }
  }

  // ========================================================================
  // Checker Framework plumbing

  public DivByZeroAnnotatedTypeFactory(BaseTypeChecker c) {
    super(c);
    postInit();
  }

  @Override
  protected TreeAnnotator createTreeAnnotator() {
    return new ListTreeAnnotator(new DivByZeroTreeAnnotator(this), super.createTreeAnnotator());
  }

  private class DivByZeroTreeAnnotator extends TreeAnnotator {

    public DivByZeroTreeAnnotator(AnnotatedTypeFactory atypeFactory) {
      super(atypeFactory);
    }

    @Override
    public Void visitLiteral(LiteralTree tree, AnnotatedTypeMirror type) {
      if (tree.getKind() == Tree.Kind.NULL_LITERAL) {
        return super.visitLiteral(tree, type);
      }
      Class<? extends Annotation> c = defaultAnnotation(tree);
      AnnotationMirror m = AnnotationBuilder.fromClass(getProcessingEnv().getElementUtils(), c);
      type.replaceAnnotation(m);
      return null;
    }

    // The AnnotatedTypeFactory only applies types computed by dataflow if they are a subtype of the
    // type it computed.  So, to get the transfer rules to work properly, we must override the
    // "output-is-lub-of-operands" behavior.  By default, everything should be Top.

    private AnnotationMirror top() {
      return getQualifierHierarchy().getTopAnnotations().iterator().next();
    }

    @Override
    public Void visitBinary(BinaryTree node, AnnotatedTypeMirror type) {
      type.replaceAnnotation(top());
      return null;
    }

    @Override
    public Void visitCompoundAssignment(CompoundAssignmentTree node, AnnotatedTypeMirror type) {
      type.replaceAnnotation(top());
      return null;
    }

    @Override
    public Void visitUnary(UnaryTree node, AnnotatedTypeMirror type) {
      type.replaceAnnotation(top());
      return null;
    }
  }
}
