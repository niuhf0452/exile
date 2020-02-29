package com.github.exile.inject.autowire

import com.github.exile.inject.Excludes
import com.github.exile.inject.Inject
import com.github.exile.inject.Named
import com.github.exile.inject.Singleton

@Inject
interface AutowireTestInterfaceA
interface AutowireTestInterfaceB
interface AutowireTestInterfaceC
abstract class AutowireTestInterfaceD
interface AutowireTestInterfaceE<A>
interface AutowireTestInterfaceF
interface AutowireTestInterfaceG
interface AutowireTestInterfaceH<A>
interface AutowireTestInterfaceI
interface AutowireTestInterfaceJ

@Inject
class TestClassA : AutowireTestInterfaceA

@Inject
@Named("foo")
class TestClassB1 : AutowireTestInterfaceB

@Inject
@Named("bar")
class TestClassB2 : AutowireTestInterfaceB

@Inject
object TestObjectC : AutowireTestInterfaceC

@Inject
class TestClassD : AutowireTestInterfaceD()

@Inject
class TestClassE1 : AutowireTestInterfaceE<String>

@Inject
class TestClassE2 : AutowireTestInterfaceE<Int>

@Inject
@Singleton
class TestClassF : AutowireTestInterfaceF

@Inject
abstract class TestClassG : AutowireTestInterfaceG

@Inject
class TestClassH<A> : AutowireTestInterfaceH<A>

class TestClassI : AutowireTestInterfaceI

@Inject
@Excludes(AutowireTestInterfaceJ::class)
class TestClassJ : AutowireTestInterfaceJ