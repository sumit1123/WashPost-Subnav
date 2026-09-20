package com.wapo.flagship.features.search2.viewmodel

import com.google.gson.Gson
import com.wapo.flagship.AppContext
import com.wapo.flagship.features.search2.model.FilterCheckItem
import com.wapo.flagship.features.search2.model.FilterRadioItem
import com.wapo.flagship.features.search2.repo.ViewModelTest
import com.washingtonpost.android.config.domain.models.config.Config
import com.washingtonpost.android.config.domain.models.config.RecipesConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito

@ExperimentalCoroutinesApi
class FilterViewModelTest : ViewModelTest() {
    private lateinit var filterViewModel: FilterViewModel

    @Mock
    lateinit var config: Config

    val recipesConfigJson =
        "{\n" +
            "        \"filters\": [\n" +
            "            {\n" +
            "                \"group\": \"Ready In\",\n" +
            "                \"queryName\": \"time\",\n" +
            "                \"isMultiSelect\": false,\n" +
            "                \"isQuickFilter\": true,\n" +
            "                \"items\": [\n" +
            "                    {\"label\": \"Any\", \"queryId\": null},\n" +
            "                    {\"label\": \"20 mins or less\", \"queryId\": \"20\"},\n" +
            "                    {\"label\": \"30 mins or less\", \"queryId\": \"30\"},\n" +
            "                    {\"label\": \"45 mins or less\", \"queryId\": \"45\"},\n" +
            "                    {\"label\": \"1 hour or less\", \"queryId\": \"60\"}\n" +
            "                ]\n" +
            "            },\n" +
            "            {\n" +
            "                \"group\": \"Course Type\",\n" +
            "                \"queryName\": \"course\",\n" +
            "                \"isMultiSelect\": true,\n" +
            "                \"isQuickFilter\": true,\n" +
            "                \"items\": [\n" +
            "                    {\"label\": \"Appetizer\", \"queryId\": \"appetizer\"},\n" +
            "                    {\"label\": \"Beverage\", \"queryId\": \"beverage\"},\n" +
            "                    {\"label\": \"Bread\", \"queryId\": \"bread\"},\n" +
            "                    {\"label\": \"Breakfast\", \"queryId\": \"breakfast\"},\n" +
            "                    {\"label\": \"Brunch\", \"queryId\": \"brunch\"},\n" +
            "                    {\"label\": \"Dessert\", \"queryId\": \"dessert\"},\n" +
            "                    {\"label\": \"Main\", \"queryId\": \"main\"},\n" +
            "                    {\"label\": \"Salad\", \"queryId\": \"salad\"},\n" +
            "                    {\"label\": \"Side\", \"queryId\": \"side\"},\n" +
            "                    {\"label\": \"Snack\", \"queryId\": \"snack\"},\n" +
            "                    {\"label\": \"Soup\", \"queryId\": \"soup\"}\n" +
            "                ]\n" +
            "            },\n" +
            "            {\n" +
            "                \"group\": \"Diet\",\n" +
            "                \"queryName\": \"diet\",\n" +
            "                \"isMultiSelect\": true,\n" +
            "                \"isQuickFilter\": true,\n" +
            "                \"items\": [\n" +
            "                    {\"label\": \"Vegan\", \"queryId\": \"vegan\"},\n" +
            "                    {\"label\": \"Vegetarian\", \"queryId\": \"vegetarian\"},\n" +
            "                    {\"label\": \"Gluten-Free\", \"queryId\": \"gluten-free\"},\n" +
            "                    {\"label\": \"Healthy\", \"queryId\": \"healthy\"}\n" +
            "                ]\n" +
            "            },\n" +
            "            {\n" +
            "                \"group\": \"Ingredients\",\n" +
            "                \"queryName\": \"ingredients\",\n" +
            "                \"isMultiSelect\": true,\n" +
            "                \"isQuickFilter\": false,\n" +
            "                \"items\": [\n" +
            "                    {\"label\": \"Apples\", \"queryId\": \"apples\"},\n" +
            "                    {\"label\": \"Beans\", \"queryId\": \"beans\"},\n" +
            "                    {\"label\": \"Beef\", \"queryId\": \"beef\"},\n" +
            "                    {\"label\": \"Cheese\", \"queryId\": \"cheese\"},\n" +
            "                    {\"label\": \"Chicken\", \"queryId\": \"chicken\"},\n" +
            "                    {\"label\": \"Chocolate\", \"queryId\": \"chocolate\"},\n" +
            "                    {\"label\": \"Eggs\", \"queryId\": \"eggs\"},\n" +
            "                    {\"label\": \"Fish\", \"queryId\": \"fish\"},\n" +
            "                    {\"label\": \"Lemon\", \"queryId\": \"lemon\"},\n" +
            "                    {\"label\": \"Mushrooms\", \"queryId\": \"mushrooms\"},\n" +
            "                    {\"label\": \"Pasta\", \"queryId\": \"pasta\"},\n" +
            "                    {\"label\": \"Pork\", \"queryId\": \"pork\"},\n" +
            "                    {\"label\": \"Potatoes\", \"queryId\": \"potatoes\"},\n" +
            "                    {\"label\": \"Rice\", \"queryId\": \"rice\"},\n" +
            "                    {\"label\": \"Salmon\", \"queryId\": \"salmon\"},\n" +
            "                    {\"label\": \"Shrimp\", \"queryId\": \"shrimp\"},\n" +
            "                    {\"label\": \"Tofu\", \"queryId\": \"tofu\"},\n" +
            "                    {\"label\": \"Tomatoes\", \"queryId\": \"tomatoes\"}\n" +
            "                ]\n" +
            "            },\n" +
            "            {\n" +
            "                \"group\": \"Feature\",\n" +
            "                \"queryName\": \"feature\",\n" +
            "                \"isMultiSelect\": true,\n" +
            "                \"isQuickFilter\": true,\n" +
            "                \"items\": [\n" +
            "                    {\"label\": \"Grilling\", \"queryId\": \"grilling\"},\n" +
            "                    {\"label\": \"Kid-friendly\", \"queryId\": \"kid-friendly\"},\n" +
            "                    {\"label\": \"Slow cooker\", \"queryId\": \"slow-cooker\"},\n" +
            "                    {\"label\": \"One-bowl baking\", \"queryId\": \"one-bowl-baking\"},\n" +
            "                    {\"label\": \"No cook\", \"queryId\": \"no-cook\"},\n" +
            "                    {\"label\": \"Make-ahead recipes\", \"queryId\": \"make-ahead-recipes\"}\n" +
            "                ]\n" +
            "            }\n" +
            "        ]\n" +
            "    }"

    @Before
    override fun setUp() {
        super.setUp()

        val recipesConfig = Gson().fromJson(recipesConfigJson, RecipesConfig::class.java)

//        Mockito.mockStatic(AppContext::class.java)
//        Mockito.`when`(AppContext.config()).thenAnswer { config }
        Mockito.`when`(config.recipesConfig).thenReturn(recipesConfig)
        filterViewModel = FilterViewModel(testCoroutineDispatcherProvider)
        filterViewModel.loadFilters(filterViewModel.recipesFilterMapCache, false)
    }

    @Test
    fun select_radio_buttons() {
        filterViewModel.loadFilters(filterViewModel.recipesFilterMapCache, false)
        filterViewModel.filterMap.observeForever {}

        selectFilterButton("time", "30")
        Assert.assertEquals("30", filterViewModel.queryFilters.filters["time"])

        selectFilterButton("time", "20")
        Assert.assertEquals("20", filterViewModel.queryFilters.filters["time"])

        selectFilterButton("time", null)
        Assert.assertEquals(null, filterViewModel.queryFilters.filters["time"])
    }

    @Test
    fun select_check_boxes() {
        filterViewModel.loadFilters(filterViewModel.recipesFilterMapCache, false)
        filterViewModel.filterMap.observeForever {}

        // Select Appetizer
        selectFilterButton("course", "appetizer")
        Assert.assertEquals("appetizer", filterViewModel.queryFilters.filters["course"])

        // Select Bread
        selectFilterButton("course", "bread")
        Assert.assertEquals("appetizer,bread", filterViewModel.queryFilters.filters["course"])

        // Select Brunch
        selectFilterButton("course", "brunch")
        Assert.assertEquals(
            "appetizer,bread,brunch",
            filterViewModel.queryFilters.filters["course"],
        )

        // Unselect Bread
        selectFilterButton("course", "bread")
        Assert.assertEquals("appetizer,brunch", filterViewModel.queryFilters.filters["course"])
    }

    /**
     * Helper function to select a specific filter.
     */
    private fun selectFilterButton(
        group: String,
        button: String?,
    ) {
        val key = filterViewModel.recipesFilterMapCache?.keys?.find { it.queryName == group }
        var value =
            filterViewModel.recipesFilterMapCache
                ?.get(key)
                ?.find { (it as? FilterRadioItem)?.queryId == button || (it as? FilterCheckItem)?.queryId == button }

        when (value) {
            is FilterRadioItem -> filterViewModel.updateRadioItemActiveFilter(value)
            is FilterCheckItem -> {
                value.isChecked = !value.isChecked
                filterViewModel.updateCheckItemActiveFilters(value)
            }

            else -> {}
        }

        println(filterViewModel.queryFilters.filters[group])
    }
}
