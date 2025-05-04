.source .\tests\HelloWorld.btm
.class public Main
.super java/lang/Object


.method public <init>()V
    .limit stack 1
    .limit locals 1
    aload_0
    invokespecial java/lang/Object/<init>()V
    return
.end method

.method public static main([Ljava/lang/String;)V
    .limit stack 10
    .limit locals 10
    new TextIO
    dup
    invokespecial TextIO/<init>()V
    ldc "Hello, World!\n"
    invokevirtual TextIO/putString(Ljava/lang/String;)LTextIO;
    return
.end method

