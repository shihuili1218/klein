/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ofcoder.klein.core.collection;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.jetbrains.annotations.NotNull;

/**
 * KleinList.
 *
 * @author 释慧利
 */
public class KleinArrayList<E> implements List<E> {
    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean contains(final Object o) {
        return false;
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return null;
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return new Object[0];
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull final T[] a) {
        return null;
    }

    @Override
    public boolean add(final E e) {
        return false;
    }

    @Override
    public void add(final int index, final E element) {

    }

    @Override
    public boolean remove(final Object o) {
        return false;
    }

    @Override
    public E remove(final int index) {
        return null;
    }

    @Override
    public boolean containsAll(@NotNull final Collection<?> c) {
        return false;
    }

    @Override
    public boolean addAll(@NotNull final Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean addAll(final int index, @NotNull final Collection<? extends E> c) {
        return false;
    }

    @Override
    public boolean removeAll(@NotNull final Collection<?> c) {
        return false;
    }

    @Override
    public boolean retainAll(@NotNull final Collection<?> c) {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public E get(final int index) {
        return null;
    }

    @Override
    public E set(final int index, final E element) {
        return null;
    }

    @Override
    public int indexOf(final Object o) {
        return 0;
    }

    @Override
    public int lastIndexOf(final Object o) {
        return 0;
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator() {
        return null;
    }

    @NotNull
    @Override
    public ListIterator<E> listIterator(final int index) {
        return null;
    }

    @NotNull
    @Override
    public List<E> subList(final int fromIndex, final int toIndex) {
        return null;
    }
}
