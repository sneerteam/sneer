/*
 * Copyright (C) 2014 LevelUp Studios
 * Copyright (C) 2007 The Android Open Source Project
 *
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
 */

package location;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Looper;
import android.text.TextUtils;

/**
 * This class provides access to the system location services, using
 * the "fused" location provider when possible (Android 4.2+). The rest
 * of the API is similar to the system {@link android.location.LocationManager LocationManager}.
 * These services allow applications to obtain periodic updates of the
 * device's geographical location, or to fire an application-specified
 * {@link Intent} when the device enters the proximity of a given
 * geographical location.
 *
 * <p>You do not
 * instantiate this class directly; instead, retrieve it through
 * {@link #getInstance(Context)}.
 *
 * <p class="note">Unless noted, all Location API methods require
 * the {@link android.Manifest.permission#ACCESS_COARSE_LOCATION} or
 * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} permissions.
 * If your application only has the coarse permission then it will not have
 * access to the GPS or passive location providers. Other providers will still
 * return location results, but the update rate will be throttled and the exact
 * location will be obfuscated to a coarse level of accuracy.
 */
public class LocationManager {

	// TODO support geofencing when available too

	private static LocationManager instance;

	public static synchronized LocationManager getInstance(Context context) {
		if (null==instance) {
			instance = new LocationManager(context);
		}
		return instance;
	}

	private final android.location.LocationManager delegate;
	private final boolean hasFused;

	private LocationManager(Context context) {
		delegate = (android.location.LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (null==delegate) throw new IllegalStateException("could not get system LocationManager");

		// determine if we can use the fused location
		boolean canHazFused = false;
		try {
			Class<?> cLocationRequest = Class.forName("android.location.LocationRequest");

			Class<?>[] paramTypes = new Class[3];
			paramTypes[0] = cLocationRequest;
			paramTypes[1] = LocationListener.class;
			paramTypes[2] = Looper.class;
			Method requestLocation = delegate.getClass().getMethod("requestLocationUpdates", paramTypes);
			if (null==requestLocation)
				throw new NullPointerException();

			Method cCreate = cLocationRequest.getMethod("create");
			Object locationRequest = cCreate.invoke(null);

			paramTypes = new Class[1];
			paramTypes[0] = int.class;
			Method mSetNumUpdates = locationRequest.getClass().getMethod("setNumUpdates", paramTypes);
			if (null==mSetNumUpdates)
				throw new NullPointerException();

			canHazFused = true;
		} catch (ClassNotFoundException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		} catch (InvocationTargetException e) {
		} catch (NullPointerException e) {
		} finally {
			this.hasFused = canHazFused;
		}
	}

    /**
     * Name of the network location provider.
     * <p>This provider determines location based on
     * availability of cell tower and WiFi access points. Results are retrieved
     * by means of a network lookup.
     */
    public static final String NETWORK_PROVIDER = "network";

    /**
     * Name of the GPS location provider.
     *
     * <p>This provider determines location using
     * satellites. Depending on conditions, this provider may take a while to return
     * a location fix. Requires the permission
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}.
     *
     * <p> The extras Bundle for the GPS location provider can contain the
     * following key/value pairs:
     * <ul>
     * <li> satellites - the number of satellites used to derive the fix
     * </ul>
     */
    public static final String GPS_PROVIDER = "gps";

    /**
     * A special location provider for receiving locations without actually initiating
     * a location fix.
     *
     * <p>This provider can be used to passively receive location updates
     * when other applications or services request them without actually requesting
     * the locations yourself.  This provider will return locations generated by other
     * providers.  You can query the {@link Location#getProvider()} method to determine
     * the origin of the location update. Requires the permission
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION}, although if the GPS is
     * not enabled this provider might only return coarse fixes.
     */
    public static final String PASSIVE_PROVIDER = "passive";

    /**
     * Name of the Fused location provider.
     *
     * <p>This provider combines inputs for all possible location sources
     * to provide the best possible Location fix. It is implicitly
     * used for all API's that involve the {@link LocationRequest}
     * object.
     */
    public static final String FUSED_PROVIDER = "fused";

    /**
     * Key used for the Bundle extra holding a boolean indicating whether
     * a proximity alert is entering (true) or exiting (false)..
     */
    public static final String KEY_PROXIMITY_ENTERING = "entering";

    /**
     * Key used for a Bundle extra holding an Integer status value
     * when a status change is broadcast using a PendingIntent.
     */
    public static final String KEY_STATUS_CHANGED = "status";

    /**
     * Key used for a Bundle extra holding an Boolean status value
     * when a provider enabled/disabled event is broadcast using a PendingIntent.
     */
    public static final String KEY_PROVIDER_ENABLED = "providerEnabled";

    /**
     * Key used for a Bundle extra holding a Location value
     * when a location change is broadcast using a PendingIntent.
     */
    public static final String KEY_LOCATION_CHANGED = "location";

    /**
     * Broadcast intent action indicating that the GPS has either been
     * enabled or disabled. An intent extra provides this state as a boolean,
     * where {@code true} means enabled.
     * @see #EXTRA_GPS_ENABLED
     *
     * @hide
     */
    public static final String GPS_ENABLED_CHANGE_ACTION =
        "android.location.GPS_ENABLED_CHANGE";

    /**
     * Broadcast intent action when the configured location providers
     * change. For use with {@link #isProviderEnabled(String)}. If you're interacting with the
     * {@link android.provider.Settings.Secure#LOCATION_MODE} API, use {@link #MODE_CHANGED_ACTION}
     * instead.
     */
    public static final String PROVIDERS_CHANGED_ACTION =
        "android.location.PROVIDERS_CHANGED";

    /**
     * Broadcast intent action when {@link android.provider.Settings.Secure#LOCATION_MODE} changes.
     * For use with the {@link android.provider.Settings.Secure#LOCATION_MODE} API.
     * If you're interacting with {@link #isProviderEnabled(String)}, use
     * {@link #PROVIDERS_CHANGED_ACTION} instead.
     *
     * In the future, there may be mode changes that do not result in
     * {@link #PROVIDERS_CHANGED_ACTION} broadcasts.
     */
    public static final String MODE_CHANGED_ACTION = "android.location.MODE_CHANGED";

	/**
	 * Returns a list of the names of all known location providers.
	 * <p>All providers are returned, including ones that are not permitted to
	 * be accessed by the calling activity or are currently disabled.
	 *
	 * @return list of Strings containing names of the provider
	 */
	public List<String> getAllProviders() {
		List<String> result = delegate.getAllProviders();
		if (hasFused)
			result.add(FUSED_PROVIDER);
		return result;
	}

	/**
	 * Returns a list of the names of location providers.
	 *
	 * @param enabledOnly if true then only the providers which are currently
	 * enabled are returned.
	 * @return list of Strings containing names of the providers
	 */
	public List<String> getProviders(boolean enabledOnly) {
		List<String> result = delegate.getProviders(enabledOnly);

		// TODO add a LocationListener on the system to get called when fused is enabled/disabled

		if (hasFused)
			result.add(FUSED_PROVIDER);
		return result;
	}

	/**
	 * Returns the information associated with the location provider of the
	 * given name, or null if no provider exists by that name.
	 *
	 * @param name the provider name
	 * @return a LocationProvider, or null
	 *
	 * @throws IllegalArgumentException if name is null or does not exist
	 * @throws SecurityException if the caller is not permitted to access the
	 * given provider.
	 */
	public LocationProvider getProvider(String name) {
		LocationProvider result = delegate.getProvider(name);
		return result;
	}

	/**
	 * Returns a list of the names of LocationProviders that satisfy the given
	 * criteria, or null if none do.  Only providers that are permitted to be
	 * accessed by the calling activity will be returned.
	 *
	 * @param criteria the criteria that the returned providers must match
	 * @param enabledOnly if true then only the providers which are currently
	 * enabled are returned.
	 * @return list of Strings containing names of the providers
	 */
	public List<String> getProviders(Criteria criteria, boolean enabledOnly) {
		List<String> result = delegate.getProviders(criteria, enabledOnly);
		// TODO verify the fused location matches the criteria
		if (hasFused)
			result.add(FUSED_PROVIDER);
		return result;
	}

	/**
	 * Returns the name of the provider that best meets the given criteria. Only providers
	 * that are permitted to be accessed by the calling activity will be
	 * returned.  If several providers meet the criteria, the one with the best
	 * accuracy is returned.  If no provider meets the criteria,
	 * the criteria are loosened in the following sequence:
	 *
	 * <ul>
	 * <li> power requirement
	 * <li> accuracy
	 * <li> bearing
	 * <li> speed
	 * <li> altitude
	 * </ul>
	 *
	 * <p> Note that the requirement on monetary cost is not removed
	 * in this process.
	 *
	 * @param criteria the criteria that need to be matched
	 * @param enabledOnly if true then only a provider that is currently enabled is returned
	 * @return name of the provider that best matches the requirements
	 */
	public String getBestProvider(Criteria criteria, boolean enabledOnly) {
		String provider = delegate.getBestProvider(criteria, enabledOnly);
		if (!hasFused)
			return provider;

		if (TextUtils.isEmpty(provider))
			return null;
		return FUSED_PROVIDER;
	}

	/**
	 * Register for location updates using the named provider, and a
	 * pending intent.
	 *
	 * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
	 * for more detail on how to use this method.
	 *
	 * @param provider the name of the provider with which to register
	 * @param minTime minimum time interval between location updates, in milliseconds
	 * @param minDistance minimum distance between location updates, in meters
	 * @param listener a {@link LocationListener} whose
	 * {@link LocationListener#onLocationChanged} method will be called for
	 * each location update
	 *
	 * @throws IllegalArgumentException if provider is null or doesn't exist
	 * on this device
	 * @throws IllegalArgumentException if listener is null
	 * @throws RuntimeException if the calling thread has no Looper
	 * @throws SecurityException if no suitable permission is present
	 */
	public void requestLocationUpdates(String provider, long minTime, float minDistance,
			LocationListener listener) {
		delegate.requestLocationUpdates(provider, minTime, minDistance, listener);
	}

	/**
	 * Register for location updates using the named provider, and a callback on
	 * the specified looper thread.
	 *
	 * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
	 * for more detail on how to use this method.
	 *
	 * @param provider the name of the provider with which to register
	 * @param minTime minimum time interval between location updates, in milliseconds
	 * @param minDistance minimum distance between location updates, in meters
	 * @param listener a {@link LocationListener} whose
	 * {@link LocationListener#onLocationChanged} method will be called for
	 * each location update
	 * @param looper a Looper object whose message queue will be used to
	 * implement the callback mechanism, or null to make callbacks on the calling
	 * thread
	 *
	 * @throws IllegalArgumentException if provider is null or doesn't exist
	 * @throws IllegalArgumentException if listener is null
	 * @throws SecurityException if no suitable permission is present
	 */
	public void requestLocationUpdates(String provider, long minTime, float minDistance,
			LocationListener listener, Looper looper) {
		delegate.requestLocationUpdates(provider, minTime, minDistance, listener, looper);
	}

	/**
	 * Register for location updates using a Criteria, and a callback
	 * on the specified looper thread.
	 *
	 * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
	 * for more detail on how to use this method.
	 *
	 * @param minTime minimum time interval between location updates, in milliseconds
	 * @param minDistance minimum distance between location updates, in meters
	 * @param criteria contains parameters for the location manager to choose the
	 * appropriate provider and parameters to compute the location
	 * @param listener a {@link LocationListener} whose
	 * {@link LocationListener#onLocationChanged} method will be called for
	 * each location update
	 * @param looper a Looper object whose message queue will be used to
	 * implement the callback mechanism, or null to make callbacks on the calling
	 * thread
	 *
	 * @throws IllegalArgumentException if criteria is null
	 * @throws IllegalArgumentException if listener is null
	 * @throws SecurityException if no suitable permission is present
	 */
	public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria,
			LocationListener listener, Looper looper) {
		delegate.requestLocationUpdates(minTime, minDistance, criteria, listener, looper);
	}

	/**
	 * Register for location updates using the named provider, and a
	 * pending intent.
	 *
	 * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
	 * for more detail on how to use this method.
	 *
	 * @param provider the name of the provider with which to register
	 * @param minTime minimum time interval between location updates, in milliseconds
	 * @param minDistance minimum distance between location updates, in meters
	 * @param intent a {@link PendingIntent} to be sent for each location update
	 *
	 * @throws IllegalArgumentException if provider is null or doesn't exist
	 * on this device
	 * @throws IllegalArgumentException if intent is null
	 * @throws SecurityException if no suitable permission is present
	 */
	public void requestLocationUpdates(String provider, long minTime, float minDistance,
			PendingIntent intent) {
		delegate.requestLocationUpdates(provider, minTime, minDistance, intent);
	}

	/**
	 * Register for location updates using a Criteria and pending intent.
	 *
	 * <p>The <code>requestLocationUpdates()</code> and
	 * <code>requestSingleUpdate()</code> register the current activity to be
	 * updated periodically by the named provider, or by the provider matching
	 * the specified {@link Criteria}, with location and status updates.
	 *
	 * <p> It may take a while to receive the first location update. If
	 * an immediate location is required, applications may use the
	 * {@link #getLastKnownLocation(String)} method.
	 *
	 * <p> Location updates are received either by {@link LocationListener}
	 * callbacks, or by broadcast intents to a supplied {@link PendingIntent}.
	 *
	 * <p> If the caller supplied a pending intent, then location updates
	 * are sent with a key of {@link #KEY_LOCATION_CHANGED} and a
	 * {@link android.location.Location} value.
	 *
	 * <p> The location update interval can be controlled using the minTime parameter.
	 * The elapsed time between location updates will never be less than
	 * minTime, although it can be more depending on the Location Provider
	 * implementation and the update interval requested by other applications.
	 *
	 * <p> Choosing a sensible value for minTime is important to conserve
	 * battery life. Each location update requires power from
	 * GPS, WIFI, Cell and other radios. Select a minTime value as high as
	 * possible while still providing a reasonable user experience.
	 * If your application is not in the foreground and showing
	 * location to the user then your application should avoid using an active
	 * provider (such as {@link #NETWORK_PROVIDER} or {@link #GPS_PROVIDER}),
	 * but if you insist then select a minTime of 5 * 60 * 1000 (5 minutes)
	 * or greater. If your application is in the foreground and showing
	 * location to the user then it is appropriate to select a faster
	 * update interval.
	 *
	 * <p> The minDistance parameter can also be used to control the
	 * frequency of location updates. If it is greater than 0 then the
	 * location provider will only send your application an update when
	 * the location has changed by at least minDistance meters, AND
	 * at least minTime milliseconds have passed. However it is more
	 * difficult for location providers to save power using the minDistance
	 * parameter, so minTime should be the primary tool to conserving battery
	 * life.
	 *
	 * <p> If your application wants to passively observe location
	 * updates triggered by other applications, but not consume
	 * any additional power otherwise, then use the {@link #PASSIVE_PROVIDER}
	 * This provider does not actively turn on or modify active location
	 * providers, so you do not need to be as careful about minTime and
	 * minDistance. However if your application performs heavy work
	 * on a location update (such as network activity) then you should
	 * select non-zero values for minTime and/or minDistance to rate-limit
	 * your update frequency in the case another application enables a
	 * location provider with extremely fast updates.
	 *
	 * <p>In case the provider is disabled by the user, updates will stop,
	 * and a provider availability update will be sent.
	 * As soon as the provider is enabled again,
	 * location updates will immediately resume and a provider availability
	 * update sent. Providers can also send status updates, at any time,
	 * with extra's specific to the provider. If a callback was supplied
	 * then status and availability updates are via
	 * {@link LocationListener#onProviderDisabled},
	 * {@link LocationListener#onProviderEnabled} or
	 * {@link LocationListener#onStatusChanged}. Alternately, if a
	 * pending intent was supplied then status and availability updates
	 * are broadcast intents with extra keys of
	 * {@link #KEY_PROVIDER_ENABLED} or {@link #KEY_STATUS_CHANGED}.
	 *
	 * <p> If a {@link LocationListener} is used but with no Looper specified
	 * then the calling thread must already
	 * be a {@link android.os.Looper} thread such as the main thread of the
	 * calling Activity. If a Looper is specified with a {@link LocationListener}
	 * then callbacks are made on the supplied Looper thread.
	 *
	 * <p class="note"> Prior to Jellybean, the minTime parameter was
	 * only a hint, and some location provider implementations ignored it.
	 * From Jellybean and onwards it is mandatory for Android compatible
	 * devices to observe both the minTime and minDistance parameters.
	 *
	 * @param minTime minimum time interval between location updates, in milliseconds
	 * @param minDistance minimum distance between location updates, in meters
	 * @param criteria contains parameters for the location manager to choose the
	 * appropriate provider and parameters to compute the location
	 * @param intent a {@link PendingIntent} to be sent for each location update
	 *
	 * @throws IllegalArgumentException if criteria is null
	 * @throws IllegalArgumentException if intent is null
	 * @throws SecurityException if no suitable permission is present
	 */
	public void requestLocationUpdates(long minTime, float minDistance, Criteria criteria,
			PendingIntent intent) {
		delegate.requestLocationUpdates(minTime, minDistance, criteria, intent);
	}

    /**
     * Register for a single location update using the named provider and
     * a callback.
     *
     * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
     * for more detail on how to use this method.
     *
     * @param provider the name of the provider with which to register
     * @param listener a {@link LocationListener} whose
     * {@link LocationListener#onLocationChanged} method will be called when
     * the location update is available
     * @param looper a Looper object whose message queue will be used to
     * implement the callback mechanism, or null to make callbacks on the calling
     * thread
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException if no suitable permission is present
     */
    public void requestSingleUpdate(String provider, LocationListener listener, Looper looper) {
    	delegate.requestSingleUpdate(provider, listener, looper);
    }

    /**
     * Register for a single location update using a Criteria and
     * a callback.
     *
     * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
     * for more detail on how to use this method.
     *
     * @param criteria contains parameters for the location manager to choose the
     * appropriate provider and parameters to compute the location
     * @param listener a {@link LocationListener} whose
     * {@link LocationListener#onLocationChanged} method will be called when
     * the location update is available
     * @param looper a Looper object whose message queue will be used to
     * implement the callback mechanism, or null to make callbacks on the calling
     * thread
     *
     * @throws IllegalArgumentException if criteria is null
     * @throws IllegalArgumentException if listener is null
     * @throws SecurityException if no suitable permission is present
     */
    public void requestSingleUpdate(Criteria criteria, LocationListener listener, Looper looper) {
    	delegate.requestSingleUpdate(criteria, listener, looper);
    }

    /**
     * Register for a single location update using a named provider and pending intent.
     *
     * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
     * for more detail on how to use this method.
     *
     * @param provider the name of the provider with which to register
     * @param intent a {@link PendingIntent} to be sent for the location update
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if intent is null
     * @throws SecurityException if no suitable permission is present
     */
    public void requestSingleUpdate(String provider, PendingIntent intent) {
    	delegate.requestSingleUpdate(provider, intent);
    }

    /**
     * Register for a single location update using a Criteria and pending intent.
     *
     * <p>See {@link #requestLocationUpdates(long, float, Criteria, PendingIntent)}
     * for more detail on how to use this method.
     *
     * @param criteria contains parameters for the location manager to choose the
     * appropriate provider and parameters to compute the location
     * @param intent a {@link PendingIntent} to be sent for the location update
     *
     * @throws IllegalArgumentException if provider is null or doesn't exist
     * @throws IllegalArgumentException if intent is null
     * @throws SecurityException if no suitable permission is present
     */
    public void requestSingleUpdate(Criteria criteria, PendingIntent intent) {
    	delegate.requestSingleUpdate(criteria, intent);
    }

	/**
	 * Removes all location updates for the specified LocationListener.
	 *
	 * <p>Following this call, updates will no longer
	 * occur for this listener.
	 *
	 * @param listener listener object that no longer needs location updates
	 * @throws IllegalArgumentException if listener is null
	 */
	public void removeUpdates(LocationListener listener) {
		delegate.removeUpdates(listener);
	}

    /**
     * Removes all location updates for the specified pending intent.
     *
     * <p>Following this call, updates will no longer for this pending intent.
     *
     * @param intent pending intent object that no longer needs location updates
     * @throws IllegalArgumentException if intent is null
     */
    public void removeUpdates(PendingIntent intent) {
    	delegate.removeUpdates(intent);
    }

    /**
     * Set a proximity alert for the location given by the position
     * (latitude, longitude) and the given radius.
     *
     * <p> When the device
     * detects that it has entered or exited the area surrounding the
     * location, the given PendingIntent will be used to create an Intent
     * to be fired.
     *
     * <p> The fired Intent will have a boolean extra added with key
     * {@link #KEY_PROXIMITY_ENTERING}. If the value is true, the device is
     * entering the proximity region; if false, it is exiting.
     *
     * <p> Due to the approximate nature of position estimation, if the
     * device passes through the given area briefly, it is possible
     * that no Intent will be fired.  Similarly, an Intent could be
     * fired if the device passes very close to the given area but
     * does not actually enter it.
     *
     * <p> After the number of milliseconds given by the expiration
     * parameter, the location manager will delete this proximity
     * alert and no longer monitor it.  A value of -1 indicates that
     * there should be no expiration time.
     *
     * <p> Internally, this method uses both {@link #NETWORK_PROVIDER}
     * and {@link #GPS_PROVIDER}.
     *
     * <p>Before API version 17, this method could be used with
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} or
     * {@link android.Manifest.permission#ACCESS_COARSE_LOCATION}.
     * From API version 17 and onwards, this method requires
     * {@link android.Manifest.permission#ACCESS_FINE_LOCATION} permission.
     *
     * @param latitude the latitude of the central point of the
     * alert region
     * @param longitude the longitude of the central point of the
     * alert region
     * @param radius the radius of the central point of the
     * alert region, in meters
     * @param expiration time for this proximity alert, in milliseconds,
     * or -1 to indicate no expiration
     * @param intent a PendingIntent that will be used to generate an Intent to
     * fire when entry to or exit from the alert region is detected
     *
     * @throws SecurityException if {@link android.Manifest.permission#ACCESS_FINE_LOCATION}
     * permission is not present
     */
    public void addProximityAlert(double latitude, double longitude, float radius, long expiration,
            PendingIntent intent) {
    	delegate.addProximityAlert(latitude, longitude, radius, expiration, intent);
    }
    /**
     * Returns the current enabled/disabled status of the given provider.
     *
     * <p>If the user has enabled this provider in the Settings menu, true
     * is returned otherwise false is returned
     *
     * <p>Callers should instead use
     * {@link android.provider.Settings.Secure#LOCATION_MODE}
     * unless they depend on provider-specific APIs such as
     * {@link #requestLocationUpdates(String, long, float, LocationListener)}.
     *
     * @param provider the name of the provider
     * @return true if the provider exists and is enabled
     *
     * @throws IllegalArgumentException if provider is null
     * @throws SecurityException if no suitable permission is present
     */
    public boolean isProviderEnabled(String provider) {
    	return delegate.isProviderEnabled(provider);
    }

    /**
     * Get the last known location.
     *
     * <p>This location could be very old so use
     * {@link Location#getElapsedRealtimeNanos} to calculate its age. It can
     * also return null if no previous location is available.
     *
     * <p>Always returns immediately.
     *
     * @return The last known location, or null if not available
     * @throws SecurityException if no suitable permission is present
     */
    @TargetApi(17)
    public Location getLastLocation() {
		try {
			Method requestLocation = delegate.getClass().getMethod("getLastLocation");
			if (null!=requestLocation)
				return (Location) requestLocation.invoke(delegate);
		} catch (NoSuchMethodException e) {
		} catch (IllegalAccessException e) {
		} catch (IllegalArgumentException e) {
		} catch (InvocationTargetException e) {
		}
		return null;
    }

	/**
	 * Returns a Location indicating the data from the last known
	 * location fix obtained from the given provider.
	 *
	 * <p> This can be done
	 * without starting the provider.  Note that this location could
	 * be out-of-date, for example if the device was turned off and
	 * moved to another location.
	 *
	 * <p> If the provider is currently disabled, null is returned.
	 *
	 * @param provider the name of the provider
	 * @return the last known location for the provider, or null
	 *
	 * @throws SecurityException if no suitable permission is present
	 * @throws IllegalArgumentException if provider is null or doesn't exist
	 */
	public Location getLastKnownLocation(String provider) {
		return delegate.getLastKnownLocation(provider);
	}

    /**
     * Adds a GPS status listener.
     *
     * @param listener GPS status listener object to register
     *
     * @return true if the listener was successfully added
     *
     * @throws SecurityException if the ACCESS_FINE_LOCATION permission is not present
     */
    public boolean addGpsStatusListener(GpsStatus.Listener listener) {
    	return delegate.addGpsStatusListener(listener);
    }

    /**
     * Removes a GPS status listener.
     *
     * @param listener GPS status listener object to remove
     */
    public void removeGpsStatusListener(GpsStatus.Listener listener) {
    	delegate.removeGpsStatusListener(listener);
    }

    /**
     * Adds an NMEA listener.
     *
     * @param listener a {@link GpsStatus.NmeaListener} object to register
     *
     * @return true if the listener was successfully added
     *
     * @throws SecurityException if the ACCESS_FINE_LOCATION permission is not present
     */
    public boolean addNmeaListener(GpsStatus.NmeaListener listener) {
    	return delegate.addNmeaListener(listener);
    }

    /**
     * Removes an NMEA listener.
     *
     * @param listener a {@link GpsStatus.NmeaListener} object to remove
     */
    public void removeNmeaListener(GpsStatus.NmeaListener listener) {
    	delegate.removeNmeaListener(listener);
    }

    /**
    * Retrieves information about the current status of the GPS engine.
    * This should only be called from the {@link GpsStatus.Listener#onGpsStatusChanged}
    * callback to ensure that the data is copied atomically.
    *
    * The caller may either pass in a {@link GpsStatus} object to set with the latest
    * status information, or pass null to create a new {@link GpsStatus} object.
    *
    * @param status object containing GPS status details, or null.
    * @return status object containing updated GPS status.
    */
   public GpsStatus getGpsStatus(GpsStatus status) {
	   return delegate.getGpsStatus(status);
   }

   /**
    * Sends additional commands to a location provider.
    * Can be used to support provider specific extensions to the Location Manager API
    *
    * @param provider name of the location provider.
    * @param command name of the command to send to the provider.
    * @param extras optional arguments for the command (or null).
    * The provider may optionally fill the extras Bundle with results from the command.
    *
    * @return true if the command succeeds.
    */
   public boolean sendExtraCommand(String provider, String command, Bundle extras) {
	   return delegate.sendExtraCommand(provider, command, extras);
   }
}
