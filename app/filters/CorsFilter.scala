package filters

import javax.inject.Inject

import play.api.http.DefaultHttpFilters
import play.filters.cors.CORSFilter

/**
  * Created by gosha-user on 12.03.2017.
  */
class CorsFilter @Inject() (corsFilter: CORSFilter) extends DefaultHttpFilters(corsFilter)
