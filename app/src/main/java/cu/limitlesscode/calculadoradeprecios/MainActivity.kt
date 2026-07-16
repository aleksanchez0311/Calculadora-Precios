package cu.limitlesscode.calculadoradeprecios

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import cu.limitlesscode.calculadoradeprecios.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager
            .findFragmentById(binding.navHostFragment.id) as NavHostFragment
        val navController = navHostFragment.navController

        // Configurar Toolbar
        setSupportActionBar(binding.toolbar)
        
        // Configurar Navegación con BottomNavigationView
        binding.bottomNavigation.setupWithNavController(navController)
        
        // Sincronizar Toolbar con Navegación
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment, R.id.managementFragment, R.id.settingsFragment)
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)
    }
}
