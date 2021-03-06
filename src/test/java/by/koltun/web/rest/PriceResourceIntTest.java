package by.koltun.web.rest;

import by.koltun.OnlinerRealtPagesApp;
import by.koltun.domain.Price;
import by.koltun.repository.PriceRepository;
import by.koltun.repository.search.PriceSearchRepository;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.Matchers.hasItem;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


/**
 * Test class for the PriceResource REST controller.
 *
 * @see PriceResource
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = OnlinerRealtPagesApp.class)
@WebAppConfiguration
@IntegrationTest
public class PriceResourceIntTest {

    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneId.of("Z"));


    private static final BigDecimal DEFAULT_PRICE_USD = new BigDecimal(1);
    private static final BigDecimal UPDATED_PRICE_USD = new BigDecimal(2);

    private static final BigDecimal DEFAULT_PRICE_RUBLE = new BigDecimal(1);
    private static final BigDecimal UPDATED_PRICE_RUBLE = new BigDecimal(2);

    private static final ZonedDateTime DEFAULT_CREATED = ZonedDateTime.ofInstant(Instant.ofEpochMilli(0L), ZoneId.systemDefault());
    private static final ZonedDateTime UPDATED_CREATED = ZonedDateTime.now(ZoneId.systemDefault()).withNano(0);
    private static final String DEFAULT_CREATED_STR = dateTimeFormatter.format(DEFAULT_CREATED);

    @Inject
    private PriceRepository priceRepository;

    @Inject
    private PriceSearchRepository priceSearchRepository;

    @Inject
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Inject
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    private MockMvc restPriceMockMvc;

    private Price price;

    @PostConstruct
    public void setup() {
        MockitoAnnotations.initMocks(this);
        PriceResource priceResource = new PriceResource();
        ReflectionTestUtils.setField(priceResource, "priceSearchRepository", priceSearchRepository);
        ReflectionTestUtils.setField(priceResource, "priceRepository", priceRepository);
        this.restPriceMockMvc = MockMvcBuilders.standaloneSetup(priceResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    @Before
    public void initTest() {
        priceSearchRepository.deleteAll();
        price = new Price();
        price.setPriceUsd(DEFAULT_PRICE_USD);
        price.setPriceRuble(DEFAULT_PRICE_RUBLE);
        price.setUpdated(DEFAULT_CREATED);
    }

    @Test
    @Transactional
    public void createPrice() throws Exception {
        int databaseSizeBeforeCreate = priceRepository.findAll().size();

        // Create the Price

        restPriceMockMvc.perform(post("/api/prices")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(price)))
                .andExpect(status().isCreated());

        // Validate the Price in the database
        List<Price> prices = priceRepository.findAll();
        assertThat(prices).hasSize(databaseSizeBeforeCreate + 1);
        Price testPrice = prices.get(prices.size() - 1);
        assertThat(testPrice.getPriceUsd()).isEqualTo(DEFAULT_PRICE_USD);
        assertThat(testPrice.getPriceRuble()).isEqualTo(DEFAULT_PRICE_RUBLE);
        assertThat(testPrice.getUpdated()).isEqualTo(DEFAULT_CREATED);

        // Validate the Price in ElasticSearch
        Price priceEs = priceSearchRepository.findOne(testPrice.getId());
        assertThat(priceEs).isEqualToComparingFieldByField(testPrice);
    }

    @Test
    @Transactional
    public void checkPriceUsdIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setPriceUsd(null);

        // Create the Price, which fails.

        restPriceMockMvc.perform(post("/api/prices")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(price)))
                .andExpect(status().isBadRequest());

        List<Price> prices = priceRepository.findAll();
        assertThat(prices).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkPriceRubleIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setPriceRuble(null);

        // Create the Price, which fails.

        restPriceMockMvc.perform(post("/api/prices")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(price)))
                .andExpect(status().isBadRequest());

        List<Price> prices = priceRepository.findAll();
        assertThat(prices).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkCreatedIsRequired() throws Exception {
        int databaseSizeBeforeTest = priceRepository.findAll().size();
        // set the field null
        price.setUpdated(null);

        // Create the Price, which fails.

        restPriceMockMvc.perform(post("/api/prices")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(price)))
                .andExpect(status().isBadRequest());

        List<Price> prices = priceRepository.findAll();
        assertThat(prices).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllPrices() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        // Get all the prices
        restPriceMockMvc.perform(get("/api/prices?sort=id,desc"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.[*].id").value(hasItem(price.getId().intValue())))
                .andExpect(jsonPath("$.[*].priceUsd").value(hasItem(DEFAULT_PRICE_USD.intValue())))
                .andExpect(jsonPath("$.[*].priceRuble").value(hasItem(DEFAULT_PRICE_RUBLE.intValue())))
                .andExpect(jsonPath("$.[*].created").value(hasItem(DEFAULT_CREATED_STR)));
    }

    @Test
    @Transactional
    public void getPrice() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);

        // Get the price
        restPriceMockMvc.perform(get("/api/prices/{id}", price.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(price.getId().intValue()))
            .andExpect(jsonPath("$.priceUsd").value(DEFAULT_PRICE_USD.intValue()))
            .andExpect(jsonPath("$.priceRuble").value(DEFAULT_PRICE_RUBLE.intValue()))
            .andExpect(jsonPath("$.created").value(DEFAULT_CREATED_STR));
    }

    @Test
    @Transactional
    public void getNonExistingPrice() throws Exception {
        // Get the price
        restPriceMockMvc.perform(get("/api/prices/{id}", Long.MAX_VALUE))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updatePrice() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);
        priceSearchRepository.save(price);
        int databaseSizeBeforeUpdate = priceRepository.findAll().size();

        // Update the price
        Price updatedPrice = new Price();
        updatedPrice.setId(price.getId());
        updatedPrice.setPriceUsd(UPDATED_PRICE_USD);
        updatedPrice.setPriceRuble(UPDATED_PRICE_RUBLE);
        updatedPrice.setUpdated(UPDATED_CREATED);

        restPriceMockMvc.perform(put("/api/prices")
                .contentType(TestUtil.APPLICATION_JSON_UTF8)
                .content(TestUtil.convertObjectToJsonBytes(updatedPrice)))
                .andExpect(status().isOk());

        // Validate the Price in the database
        List<Price> prices = priceRepository.findAll();
        assertThat(prices).hasSize(databaseSizeBeforeUpdate);
        Price testPrice = prices.get(prices.size() - 1);
        assertThat(testPrice.getPriceUsd()).isEqualTo(UPDATED_PRICE_USD);
        assertThat(testPrice.getPriceRuble()).isEqualTo(UPDATED_PRICE_RUBLE);
        assertThat(testPrice.getUpdated()).isEqualTo(UPDATED_CREATED);

        // Validate the Price in ElasticSearch
        Price priceEs = priceSearchRepository.findOne(testPrice.getId());
        assertThat(priceEs).isEqualToComparingFieldByField(testPrice);
    }

    @Test
    @Transactional
    public void deletePrice() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);
        priceSearchRepository.save(price);
        int databaseSizeBeforeDelete = priceRepository.findAll().size();

        // Get the price
        restPriceMockMvc.perform(delete("/api/prices/{id}", price.getId())
                .accept(TestUtil.APPLICATION_JSON_UTF8))
                .andExpect(status().isOk());

        // Validate ElasticSearch is empty
        boolean priceExistsInEs = priceSearchRepository.exists(price.getId());
        assertThat(priceExistsInEs).isFalse();

        // Validate the database is empty
        List<Price> prices = priceRepository.findAll();
        assertThat(prices).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void searchPrice() throws Exception {
        // Initialize the database
        priceRepository.saveAndFlush(price);
        priceSearchRepository.save(price);

        // Search the price
        restPriceMockMvc.perform(get("/api/_search/prices?query=id:" + price.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.[*].id").value(hasItem(price.getId().intValue())))
            .andExpect(jsonPath("$.[*].priceUsd").value(hasItem(DEFAULT_PRICE_USD.intValue())))
            .andExpect(jsonPath("$.[*].priceRuble").value(hasItem(DEFAULT_PRICE_RUBLE.intValue())))
            .andExpect(jsonPath("$.[*].created").value(hasItem(DEFAULT_CREATED_STR)));
    }
}
