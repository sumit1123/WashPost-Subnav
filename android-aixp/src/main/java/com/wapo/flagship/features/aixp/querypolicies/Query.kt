/* Copyright (c) 2024 The Washington Post. All rights reserved. */
package com.wapo.flagship.features.aixp.querypolicies

/**
 * A class that holds all the information to get data from the repository
 * @param url the url to the resource you want to retrieve
 * @param queryPolicy a QueryPolicy implementation that tells the repository how you want to retrieve the data
 */
class Query<T>(val url: String, val queryPolicy: QueryPolicy<T> = DefaultQueryPolicy())