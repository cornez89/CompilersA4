.source .\tests\ArrayIndexNegative.btm
.class public Main
.super java/lang/Object
.implements java/lang/Cloneable


.method public <init>()V
    .limit stack 1
    .limit locals 1
    aload_0
    invokespecial java/lang/Object/<init>()V
    return
.end method

.method public static main([Ljava/lang/String;)V
.throws java/lang/CloneNotSupportedException
    .limit stack 10
    .limit locals 10
    ; Declaration array : int[]
    iconst_3
    newarray int
    astore 1
    aload 1
    iconst_0
    iconst_0
    iastore
    aload 1
    iconst_1
    ineg
    iconst_1
    iastore
    return
.end method

