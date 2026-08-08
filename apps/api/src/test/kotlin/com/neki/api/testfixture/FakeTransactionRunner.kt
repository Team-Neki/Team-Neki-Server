package com.neki.api.testfixture

import com.neki.core.transaction.TransactionRunner

class FakeTransactionRunner : TransactionRunner {
    override fun <T> run(func: () -> T): T = func()
    override fun <T> readOnly(func: () -> T): T = func()
    override fun <T> runNew(func: () -> T): T = func()
}
