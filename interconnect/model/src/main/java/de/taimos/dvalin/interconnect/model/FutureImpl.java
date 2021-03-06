/**
 *
 */
package de.taimos.dvalin.interconnect.model;

/*
 * #%L
 * Dvalin interconnect transfer data model
 * %%
 * Copyright (C) 2016 Taimos GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.EventListener;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @param <T> the return type
 */
public final class FutureImpl<T> implements Future<T> {

	private static final Object CANCELLED_FLAG = new Object(); // if this is the value of this.value.get() than cancel() was called.

	private final UUID id = UUID.randomUUID();

	private final CountDownLatch cdl = new CountDownLatch(1);

	private final AtomicReference<Object> value = new AtomicReference<>(null);

	private final Set<CancelListener<T>> listeners = new CopyOnWriteArraySet<>();


	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (this.value.compareAndSet(null, FutureImpl.CANCELLED_FLAG)) {
			try {
				for (final CancelListener<T> listener : this.listeners) {
					listener.wasCancelled(this.getId());
				}
				return true;
			} finally {
				this.listeners.clear();
				this.cdl.countDown();
			}
		}
		return false;
	}

	@Override
	public boolean isCancelled() {
		return FutureImpl.CANCELLED_FLAG.equals(this.value.get());
	}

	@Override
	public boolean isDone() {
		return this.cdl.getCount() == 0;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get() throws InterruptedException, ExecutionException, CancellationException {
		this.cdl.await();
		final Object o = this.value.get();
		if (FutureImpl.CANCELLED_FLAG.equals(o)) {
			throw new CancellationException();
		}
		if (o instanceof Exception) {
			throw new ExecutionException((Exception) o);
		}
		return (T) o;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException, CancellationException {
		if (!this.cdl.await(timeout, unit)) {
			throw new TimeoutException();
		}
		final Object o = this.value.get();
		if (FutureImpl.CANCELLED_FLAG.equals(o)) {
			throw new CancellationException();
		}
		if (o instanceof Exception) {
			throw new ExecutionException((Exception) o);
		}
		return (T) o;
	}

	/**
	 * @param value Value
	 * @deprecated Use set(value) instead
	 */
	@Deprecated
	public void setResult(final T value) {
		this.set(value);
	}

	/**
	 * All callers to get() are now returned.<br>
	 * Multiple calls are ignored.<br>
	 * Calls after cancel are ignored.
	 *
	 * @param value Value
	 */
	public void set(final T value) {
		if (this.value.compareAndSet(null, value)) {
			this.listeners.clear();
			this.cdl.countDown();
		}
	}

	/**
	 * The get() method throws this exception wrapped as the cause of an ExecutionException.<br>
	 * Multiple calls are ignored.<br>
	 * Calls after cancel are ignored.
	 *
	 * @param e Exception
	 */
	public void set(final Exception e) {
		if (this.value.compareAndSet(null, e)) {
			this.listeners.clear();
			this.cdl.countDown();
		}
	}

	/**
	 * @return the id
	 */
	public UUID getId() {
		return this.id;
	}


	/**
	 * @param <T> the return type
	 */
	public interface CancelListener<T> extends EventListener {

		/**
		 * @param id Future id
		 */
		void wasCancelled(final UUID id);
	}


	/**
	 * IMPORTANT: In the event that the Cancel listener is added after cancel() was called the event is immediately fired.<br>
	 * IMPORTANT2: Keep in mind, that the Cancel listener may not be called from your own thread, so you must care about thread-safety!
	 *
	 * @param listener Cancel listener
	 */
	public void addCancelListener(final CancelListener<T> listener) {
		final Object v = this.value.get();
		if (v == null) {
			this.listeners.add(listener);
		} else if (FutureImpl.CANCELLED_FLAG.equals(v)) {
			listener.wasCancelled(this.getId());
		}
	}

	/**
	 * @param listener Cancel listener
	 */
	public void removeCancelListener(final CancelListener<T> listener) {
		if (this.value.get() == null) {
			this.listeners.remove(listener);
		}
	}
}
