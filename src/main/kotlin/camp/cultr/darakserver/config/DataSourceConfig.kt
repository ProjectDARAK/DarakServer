package camp.cultr.darakserver.config

import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import javax.sql.DataSource

@Configuration
@EnableJpaRepositories(
    basePackages = ["camp.cultr.darakserver.repository"],
    entityManagerFactoryRef = "darakDbEntityManagerFactory",
    transactionManagerRef = "darakDbTransactionManager"
)
@EnableJpaAuditing
class DataSourceConfig {

    @Bean
    @Primary
    fun darakDataSourceProperties(
        prop: DarakDatabaseProperties,
    ) = DataSourceProperties().apply {
        url = "jdbc:postgresql://${prop.url}:${prop.port}/${prop.database}"
        username = prop.username
        password = prop.password
        driverClassName = "org.postgresql.Driver"
    }

    @Bean
    @Primary
    fun darakDataSourceConfig(darakDataSourceProp: DataSourceProperties): DataSource = darakDataSourceProp
        .initializeDataSourceBuilder()
        .type(HikariDataSource::class.java)
        .build()

    @Bean(name = ["darakDbEntityManagerFactory"])
    @Primary
    fun darakDbEntityManagerFactory(
        builder: EntityManagerFactoryBuilder,
        darakDataSource: DataSource
    ): LocalContainerEntityManagerFactoryBean =
        builder
            .dataSource(darakDataSource)
            .packages("camp.cultr.darakserver.domain")
            .persistenceUnit("darakDbEntityManager")
            .build()

    @Bean(name = ["darakDbTransactionManager"])
    @Primary
    fun darakDbTransactionManager(
        @Qualifier("darakDbEntityManagerFactory") localContainerEntityManagerFactoryBean: LocalContainerEntityManagerFactoryBean,
    ) = JpaTransactionManager(localContainerEntityManagerFactoryBean.`object`!!)
}

@ConfigurationProperties("darak.database")
data class DarakDatabaseProperties(
    val url: String = "localhost",
    val username: String = "darak_test",
    val password: String = "ThisIsNotARealPassword",
    val port: Int = 5432,
    val database: String = "darak"
)