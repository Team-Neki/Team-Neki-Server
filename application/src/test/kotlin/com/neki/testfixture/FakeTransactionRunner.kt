package com.neki.testfixture

import com.neki.common.transaction.TransactionRunner

class FakeTransactionRunner : TransactionRunner {
    override fun <T> run(func: () -> T): T = func()
    override fun <T> readOnly(func: () -> T): T = func()
    override fun <T> runNew(func: () -> T): T = func()
}
