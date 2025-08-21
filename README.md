# Metro IC bug

This branch highlights an incremental compilation issue when using Metro. There's a high chance that this bug happens 
within kotlinc. [This log is printed in the console](https://scans.gradle.com/s/grmlymleqhfh4/console-log?page=1#L481), 
but the Gradle build itself is successful. Very concerning is that the build output is not correct and the code changes
that trigger the incremental compilation issue are not applied.
```
[KOTLIN] [IC] Incremental compilation was attempted but failed:	
    Failed to compute files to recompile: java.lang.IllegalStateException: The following LookupSymbols are not yet converted to ProgramSymbols: LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateKey.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplate.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplate.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplateKey.)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.toProgramSymbols(ClasspathChangesComputer.kt:361)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeFineGrainedKotlinClassChanges(ClasspathChangesComputer.kt:264)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeKotlinClassChanges(ClasspathChangesComputer.kt:164)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeClassChanges(ClasspathChangesComputer.kt:137)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeChangedAndImpactedSet(ClasspathChangesComputer.kt:87)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeClasspathChanges(ClasspathChangesComputer.kt:55)	
	at org.jetbrains.kotlin.incremental.dirtyFiles.ClasspathSnapshotBasedImpactDeterminer.determineChangedAndImpactedSymbols(ClasspathSnapshotBasedImpactDeterminer.kt:42)	
	at org.jetbrains.kotlin.incremental.dirtyFiles.JvmSourcesToCompileCalculator.calculateSourcesToCompileImpl(JvmSourcesToCompileCalculator.kt:41)	
	at org.jetbrains.kotlin.incremental.dirtyFiles.JvmSourcesToCompileCalculator.calculateWithClasspathSnapshot(JvmSourcesToCompileCalculator.kt:97)	
	at org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner.calculateSourcesToCompile(IncrementalJvmCompilerRunner.kt:60)	
	at org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner.calculateSourcesToCompile(IncrementalJvmCompilerRunner.kt:23)	
	at org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.tryCompileIncrementally$lambda$10$compile(IncrementalCompilerRunner.kt:230)	
	at org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.tryCompileIncrementally(IncrementalCompilerRunner.kt:272)	
	at org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.compile(IncrementalCompilerRunner.kt:124)	
	at org.jetbrains.kotlin.daemon.CompileServiceImplBase.execIncrementalCompiler(CompileServiceImpl.kt:679)	
	at org.jetbrains.kotlin.daemon.CompileServiceImplBase.access$execIncrementalCompiler(CompileServiceImpl.kt:93)	
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.compile(CompileServiceImpl.kt:1806)	
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)	
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)	
	at java.rmi/sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:360)	
	at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:200)	
	at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:197)	
	at java.base/java.security.AccessController.doPrivileged(AccessController.java:714)	
	at java.rmi/sun.rmi.transport.Transport.serviceCall(Transport.java:196)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:598)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:844)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:721)	
	at java.base/java.security.AccessController.doPrivileged(AccessController.java:400)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:720)	
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)	
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)	
	at java.base/java.lang.Thread.run(Thread.java:1583)	
    Falling back to non-incremental compilation (reason = IC_FAILED_TO_COMPUTE_FILES_TO_RECOMPILE)	
    To help us fix this issue, please file a bug at https://youtrack.jetbrains.com/issues/KT with the above stack trace.	
    (Be sure to search for the above exception in existing issues first to avoid filing duplicated bugs.)             	
:recipes:app:mergeLibDexDebug	
e: Incremental compilation failed: The following LookupSymbols are not yet converted to ProgramSymbols: LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateKey.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplate.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplate.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplateKey.)	
java.lang.IllegalStateException: The following LookupSymbols are not yet converted to ProgramSymbols: LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateKey.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplate.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplate.), LookupSymbol(name=MetroFactory, scope=app.platform.inject.metro.software.amazon.app.platform.recipes.template.RootPresenterRendererComponent.ProvideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplateKey.)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.toProgramSymbols(ClasspathChangesComputer.kt:361)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeFineGrainedKotlinClassChanges(ClasspathChangesComputer.kt:264)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeKotlinClassChanges(ClasspathChangesComputer.kt:164)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeClassChanges(ClasspathChangesComputer.kt:137)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeChangedAndImpactedSet(ClasspathChangesComputer.kt:87)	
	at org.jetbrains.kotlin.incremental.classpathDiff.ClasspathChangesComputer.computeClasspathChanges(ClasspathChangesComputer.kt:55)	
	at org.jetbrains.kotlin.incremental.dirtyFiles.ClasspathSnapshotBasedImpactDeterminer.determineChangedAndImpactedSymbols(ClasspathSnapshotBasedImpactDeterminer.kt:42)	
	at org.jetbrains.kotlin.incremental.dirtyFiles.JvmSourcesToCompileCalculator.calculateSourcesToCompileImpl(JvmSourcesToCompileCalculator.kt:41)	
	at org.jetbrains.kotlin.incremental.dirtyFiles.JvmSourcesToCompileCalculator.calculateWithClasspathSnapshot(JvmSourcesToCompileCalculator.kt:97)	
	at org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner.calculateSourcesToCompile(IncrementalJvmCompilerRunner.kt:60)	
	at org.jetbrains.kotlin.incremental.IncrementalJvmCompilerRunner.calculateSourcesToCompile(IncrementalJvmCompilerRunner.kt:23)	
	at org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.tryCompileIncrementally$lambda$10$compile(IncrementalCompilerRunner.kt:230)	
	at org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.tryCompileIncrementally(IncrementalCompilerRunner.kt:272)	
	at org.jetbrains.kotlin.incremental.IncrementalCompilerRunner.compile(IncrementalCompilerRunner.kt:124)	
	at org.jetbrains.kotlin.daemon.CompileServiceImplBase.execIncrementalCompiler(CompileServiceImpl.kt:679)	
	at org.jetbrains.kotlin.daemon.CompileServiceImplBase.access$execIncrementalCompiler(CompileServiceImpl.kt:93)	
	at org.jetbrains.kotlin.daemon.CompileServiceImpl.compile(CompileServiceImpl.kt:1806)	
	at java.base/jdk.internal.reflect.DirectMethodHandleAccessor.invoke(DirectMethodHandleAccessor.java:103)	
	at java.base/java.lang.reflect.Method.invoke(Method.java:580)	
	at java.rmi/sun.rmi.server.UnicastServerRef.dispatch(UnicastServerRef.java:360)	
	at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:200)	
	at java.rmi/sun.rmi.transport.Transport$1.run(Transport.java:197)	
	at java.base/java.security.AccessController.doPrivileged(AccessController.java:714)	
	at java.rmi/sun.rmi.transport.Transport.serviceCall(Transport.java:196)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport.handleMessages(TCPTransport.java:598)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run0(TCPTransport.java:844)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.lambda$run$0(TCPTransport.java:721)	
	at java.base/java.security.AccessController.doPrivileged(AccessController.java:400)	
	at java.rmi/sun.rmi.transport.tcp.TCPTransport$ConnectionHandler.run(TCPTransport.java:720)	
	at java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)	
	at java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)	
	at java.base/java.lang.Thread.run(Thread.java:1583)
```

## Reproducing the bug

1. Run the Gradle build `./gradlew :recipes:app:assembleDebug`.
2. In `recipes/common/impl/src/commonMain/kotlin/software/amazon/app/platform/recipes/template/RootPresenterRenderer.kt`
remove line 48, the `@ContributesRenderer` annotation.
3. Run the Gradle build again `./gradlew :recipes:app:assembleDebug`.

## Noteworthy details

* This is a KMP project. This issue happens when building for the JVM and Android. I haven't tested other platforms yet.
* `@ContributesRenderer` is a custom annotation. A KSP processor generates this file. When the `@ContributesRenderer` 
annotation is removed, this file gets removed as well.
```kotlin
@ContributesTo(RendererScope::class)
public interface RootPresenterRendererComponent {
  @Provides
  @IntoMap
  @RendererKey(RecipesAppTemplate::class)
  public fun provideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplate(renderer: Provider<RootPresenterRenderer>): Renderer<*> = renderer()

  @Provides
  @IntoMap
  @RendererKey(RecipesAppTemplate.FullScreenTemplate::class)
  public fun provideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplate(renderer: Provider<RootPresenterRenderer>): Renderer<*> = renderer()

  @Provides
  @IntoMap
  @RendererKey(RecipesAppTemplate::class)
  @ForScope(scope = RendererScope::class)
  public fun provideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateKey(): KClass<out Renderer<*>> = RootPresenterRenderer::class

  @Provides
  @IntoMap
  @RendererKey(RecipesAppTemplate.FullScreenTemplate::class)
  @ForScope(scope = RendererScope::class)
  public fun provideSoftwareAmazonAppPlatformRecipesTemplateRootPresenterRendererRecipesAppTemplateFullScreenTemplateKey(): KClass<out Renderer<*>> = RootPresenterRenderer::class
}
```
* The file `RootPresenterRenderer` is part of the `:recipes:common:impl` library module, but the IC bug happens in 
`:recipes:app`. The app module imports the library module. 

## Metro ticket

https://github.com/ZacSweers/metro/issues/997
