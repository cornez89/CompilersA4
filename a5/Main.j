.source .\tests\Assign.btm
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
    ; Declaration io : TextIO
    new TextIO
    dup
    invokespecial TextIO/<init>()V
    astore 1
    ; Declaration x : int
    iconst_0
    istore_1
    ; Declaration y : int
    iconst_1
    istore_1
    ; Declaration z : int
    iconst_2
    istore_1
    aload 1
    ldc "x="
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    iload_1
    ldc "\n"
    aload 1
    ldc "y="
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    iload_1
    ldc "\n"
    aload 1
    ldc "z="
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    iload_1
    ldc "\n"
    aload 1
    ldc "Executing: x = y = z\n"
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    iload_1
    istore_1
    istore_1
    aload 1
    ldc "x="
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    iload_1
    ldc "\n"
    aload 1
    ldc "y="
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    iload_1
    ldc "\n"
    aload 1
    ldc "z="
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    iload_1
    ldc "\n"
    return
.end method

