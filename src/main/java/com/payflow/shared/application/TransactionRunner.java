package com.payflow.shared.application;

/**

 * Application boundary for executing work atomically.
 *
 * <p>The concrete infrastructure implementation will later map this
 * boundary to a database transaction, for example using Spring's
 * transaction management.</p>
 */
@FunctionalInterface
public interface TransactionRunner {

    /**

     * Executes the supplied work inside an application transaction.
     *
     * @param work operation to execute
     */
    void execute(Runnable work);
}
