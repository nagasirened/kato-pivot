package com.kato.pro.common.utils;

import cn.hutool.core.map.MapUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Lambdas {

    /**
     * Collection 转化为 Map
     * ========================================================
     */
    public static <T, K> Map<K, T> toMap(Collection<T> collection, Function<? super T, ? extends K> keyMapper) {
        return toMap(collection, keyMapper, Function.identity());
    }

    public static <T, K, V> Map<K, V> toMap(Collection<T> collection,
                                            Function<? super T, ? extends K> keyMapper,
                                            Function<? super T, ? extends V> valueMapper) {
        return toMap(collection, keyMapper, valueMapper, pickSecond());
    }

    public static <T, K, V> Map<K, V> toMap(Collection<T> collection,
                                            Function<? super T, ? extends K> keyMapper,
                                            Function<? super T, ? extends V> valueMapper,
                                            BinaryOperator<V> mergeFunction) {
        return collection.stream().collect(Collectors.toMap(keyMapper, valueMapper, mergeFunction));
    }

    public static <T> BinaryOperator<T> pickFirst() {
        return (t1, t2) -> t1;
    }

    public static <T> BinaryOperator<T> pickSecond() {
        return (t1, t2) -> t2;
    }


    /**
     * Map格式转换
     * ========================================================
     */
    public static <K, V, C> Map<K, C> convertMapValue(Map<K, V> map, BiFunction<K, V, C> valueFunction) {
        return convertMapValue(map, valueFunction, pickSecond());
    }

    public static <K, V, C> Map<K, C> convertMapValue(Map<K, V> map,
                                                      BiFunction<K, V, C> valueFunction,
                                                      BinaryOperator<C> mergeFunction) {
        if (MapUtil.isEmpty(map)) {
            return new HashMap<>();
        }
        return map.entrySet().stream().collect(
                Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> valueFunction.apply(entry.getKey(), entry.getValue()),
                        mergeFunction
                )
        );
    }

    /**
     * 集合类型转化
     * ========================================================
     */
    public static <T> List<T> toList(Collection<T> collection) {
        return toList(collection, false);
    }

    public static <T> List<T> toList(Collection<T> collection, boolean distinct) {
        if (collection == null) {
            return new ArrayList<>();
        }
        if (collection instanceof List) {
            return (List<T>) collection;
        }
        Stream<T> stream = collection.stream();
        if (distinct) {
            stream = stream.distinct();
        }
        return stream.collect(Collectors.toList());
    }

    public static <T> Set<T> toSet(Collection<T> collection) {
        if (collection == null) {
            return new HashSet<>();
        }
        if (collection instanceof List) {
            return (Set<T>) collection;
        }
        return new HashSet<>(collection);
    }

    /**
     * List、Set 类型之间的转换
     * ========================================================
     */
    public static <T, R> List<R> mapping(List<T> collection, Function<T, R> mapper) {
        return mappingToList(collection, mapper);
    }

    public static <T, R> Set<R> mapping(Set<T> collection, Function<T, R> mapper) {
        return mappingToSet(collection, mapper);
    }

    public static <T, R> List<R> mappingToList(Collection<T> collection, Function<T, R> mapper) {
        return collection.stream().map(mapper).collect(Collectors.toList());
    }

    public static <T, R> Set<R> mappingToSet(Collection<T> collection, Function<T, R> mapper) {
        return collection.stream().map(mapper).collect(Collectors.toSet());
    }

}


